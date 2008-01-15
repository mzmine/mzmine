/*
 * Copyright 2006-2007 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.normalization.standardcompound;

import java.util.logging.Logger;

import net.sf.mzmine.data.CompoundIdentity;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimplePeak;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.normalization.linear.LinearNormalizerParameters;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;

public class StandardCompoundNormalizerTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private PeakList originalPeakList;

    private TaskStatus taskStatus = TaskStatus.WAITING;
    private String errorMessage;

    private int processedRows, totalRows;

    private String suffix, normalizationType, peakMeasurementType;
    private float MZvsRTBalance;
    private boolean removeOriginal;
    private PeakListRow[] standardRows;

    public StandardCompoundNormalizerTask(PeakList peakList,
            StandardCompoundNormalizerParameters parameters) {

        this.originalPeakList = peakList;

        suffix = (String) parameters.getParameterValue(LinearNormalizerParameters.suffix);
        normalizationType = (String) parameters.getParameterValue(StandardCompoundNormalizerParameters.standardUsageType);
        peakMeasurementType = (String) parameters.getParameterValue(StandardCompoundNormalizerParameters.peakMeasurementType);
        MZvsRTBalance = (Float) parameters.getParameterValue(StandardCompoundNormalizerParameters.MZvsRTBalance);
        removeOriginal = (Boolean) parameters.getParameterValue(StandardCompoundNormalizerParameters.autoRemove);
        standardRows = parameters.getSelectedStandardPeakListRows();
        
    }

    public void cancel() {
        taskStatus = TaskStatus.CANCELED;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public float getFinishedPercentage() {
        if (totalRows == 0)
            return 0f;
        return (float) processedRows / (float) totalRows;
    }

    public TaskStatus getStatus() {
        return taskStatus;
    }

    public String getTaskDescription() {
        return "Standard compound normalization of " + originalPeakList;
    }

    public void run() {

        taskStatus = TaskStatus.PROCESSING;

        logger.finest("Starting standard compound normalization of "
                + originalPeakList + " using " + normalizationType + " (total "
                + standardRows.length + " standard peaks)");

        // Initialize new alignment result for the normalized result
        SimplePeakList normalizedPeakList = new SimplePeakList(originalPeakList + " " + suffix);

        // Copy raw data files from original alignment result to new alignment
        // result
        totalRows = originalPeakList.getNumberOfRows();

        // Loop through all rows
        for (PeakListRow row : originalPeakList.getRows()) {

            // Cancel ?
            if (taskStatus == TaskStatus.CANCELED) {
                return;
            }

            SimplePeakListRow normalizedRow = new SimplePeakListRow(row.getID());
            normalizedRow.setComment(row.getComment());
            for (CompoundIdentity ident : row.getCompoundIdentities())
                normalizedRow.addCompoundIdentity(ident);

            // Get m/z and RT of the current row
            float mz = row.getAverageMZ();
            float rt = row.getAverageRT();

            // Loop through all raw data files
            for (RawDataFile file : originalPeakList.getRawDataFiles()) {

                float normalizationFactors[] = null;
                float normalizationFactorWeights[] = null;

                if (normalizationType == StandardCompoundNormalizerParameters.standardUsageTypeNearest) {

                    // Search for nearest standard
                    PeakListRow nearestStandardRow = null;
                    float nearestStandardRowDistance = Float.MAX_VALUE;

                    for (int standardRowIndex = 0; standardRowIndex < standardRows.length; standardRowIndex++) {
                        PeakListRow standardRow = standardRows[standardRowIndex];

                        float stdMZ = standardRow.getAverageMZ();
                        float stdRT = standardRow.getAverageRT();
                        float distance = MZvsRTBalance * Math.abs(mz - stdMZ)
                                + Math.abs(rt - stdRT);
                        if (distance <= nearestStandardRowDistance) {
                            nearestStandardRow = standardRow;
                            nearestStandardRowDistance = distance;
                        }

                    }

                    // Calc and store a single normalization factor
                    normalizationFactors = new float[1];
                    normalizationFactorWeights = new float[1];
                    Peak standardPeak = nearestStandardRow.getPeak(file);
                    if (peakMeasurementType == StandardCompoundNormalizerParameters.peakMeasurementTypeHeight) {
                        normalizationFactors[0] = standardPeak.getHeight();
                    } else {
                        normalizationFactors[0] = standardPeak.getArea();
                    }
                    logger.finest("Normalizing using standard peak "
                            + standardPeak + ", factor "
                            + normalizationFactors[0]);
                    normalizationFactorWeights[0] = 1.0f;

                }

                if (normalizationType == StandardCompoundNormalizerParameters.standardUsageTypeWeighted) {

                    // Add all standards as factors, and use distance as weight
                    normalizationFactors = new float[standardRows.length];
                    normalizationFactorWeights = new float[standardRows.length];

                    for (int standardRowIndex = 0; standardRowIndex < standardRows.length; standardRowIndex++) {
                        PeakListRow standardRow = standardRows[standardRowIndex];

                        float stdMZ = standardRow.getAverageMZ();
                        float stdRT = standardRow.getAverageRT();
                        float distance = MZvsRTBalance * Math.abs(mz - stdMZ)
                                + Math.abs(rt - stdRT);

                        Peak standardPeak = standardRow.getPeak(file);
                        if (standardPeak == null) {
                            // What to do if standard peak is not
                            // available? (Currently this is ruled out by the
                            // setup dialog, which shows only peaks that are
                            // present in all samples)
                            normalizationFactors[standardRowIndex] = 1.0f;
                            normalizationFactorWeights[standardRowIndex] = 0.0f;
                        } else {
                            if (peakMeasurementType == StandardCompoundNormalizerParameters.peakMeasurementTypeHeight) {
                                normalizationFactors[standardRowIndex] = standardPeak.getHeight();
                            } else {
                                normalizationFactors[standardRowIndex] = standardPeak.getArea();
                            }
                            normalizationFactorWeights[standardRowIndex] = 1 / distance;
                        }
                    }

                }

                // Calculate a single normalization factor as weighted average
                // of all factors
                float weightedSum = 0.0f;
                float sumOfWeights = 0.0f;
                for (int factorIndex = 0; factorIndex < normalizationFactors.length; factorIndex++) {
                    weightedSum += normalizationFactors[factorIndex]
                            * normalizationFactorWeights[factorIndex];
                    sumOfWeights += normalizationFactorWeights[factorIndex];
                }
                float normalizationFactor = weightedSum / sumOfWeights;

                // For simple scaling of the normalized values
                normalizationFactor = normalizationFactor / 100.0f;

                logger.finest("Normalizing row " + row + "[" + file
                        + "] using factor " + normalizationFactor);

                // How to handle zero normalization factor?
                if (normalizationFactor == 0.0)
                    normalizationFactor = Float.MIN_VALUE;

                // Normalize peak
                Peak originalPeak = row.getPeak(file);
                if (originalPeak != null) {
                    SimplePeak normalizedPeak = new SimplePeak(originalPeak);
                    float normalizedHeight = originalPeak.getHeight()
                            / normalizationFactor;
                    float normalizedArea = originalPeak.getArea()
                            / normalizationFactor;
                    normalizedPeak.setHeight(normalizedHeight);
                    normalizedPeak.setArea(normalizedArea);

                    normalizedRow.addPeak(file, originalPeak, normalizedPeak);
                }

            }

            normalizedPeakList.addRow(normalizedRow);
            processedRows++;

        }

        // Add new peaklist to the project
        MZmineProject currentProject = MZmineCore.getCurrentProject();
        currentProject.addPeakList(normalizedPeakList);

        // Remove the original peaklist if requested
        if (removeOriginal)
            currentProject.removePeakList(originalPeakList);

        taskStatus = TaskStatus.FINISHED;

    }

}
