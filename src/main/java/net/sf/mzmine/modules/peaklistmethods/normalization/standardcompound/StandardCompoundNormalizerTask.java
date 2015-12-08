/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.normalization.standardcompound;

import java.util.logging.Logger;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.normalization.linear.LinearNormalizerParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakMeasurementType;
import net.sf.mzmine.util.PeakUtils;

public class StandardCompoundNormalizerTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final MZmineProject project;
    private PeakList originalPeakList, normalizedPeakList;

    private int processedRows, totalRows;

    private String suffix;
    private StandardUsageType normalizationType;
    private PeakMeasurementType peakMeasurementType;
    private double MZvsRTBalance;
    private boolean removeOriginal;
    private PeakListRow[] standardRows;
    private ParameterSet parameters;

    public StandardCompoundNormalizerTask(MZmineProject project,
            PeakList peakList, ParameterSet parameters) {

        this.project = project;
        this.originalPeakList = peakList;

        suffix = parameters.getParameter(LinearNormalizerParameters.suffix)
                .getValue();
        normalizationType = parameters
                .getParameter(
                        StandardCompoundNormalizerParameters.standardUsageType)
                .getValue();
        peakMeasurementType = parameters
                .getParameter(
                        StandardCompoundNormalizerParameters.peakMeasurementType)
                .getValue();
        MZvsRTBalance = parameters
                .getParameter(
                        StandardCompoundNormalizerParameters.MZvsRTBalance)
                .getValue();
        removeOriginal = parameters
                .getParameter(StandardCompoundNormalizerParameters.autoRemove)
                .getValue();
        standardRows = parameters
                .getParameter(
                        StandardCompoundNormalizerParameters.standardCompounds)
                .getMatchingRows(peakList);

    }

    public double getFinishedPercentage() {
        if (totalRows == 0)
            return 0;
        return (double) processedRows / (double) totalRows;
    }

    public String getTaskDescription() {
        return "Standard compound normalization of " + originalPeakList;
    }

    public void run() {

        setStatus(TaskStatus.PROCESSING);

        logger.finest("Starting standard compound normalization of "
                + originalPeakList + " using " + normalizationType + " (total "
                + standardRows.length + " standard peaks)");

        // Check if we have standards
        if (standardRows.length == 0) {
            setErrorMessage("No internal standard peaks selected");
            setStatus(TaskStatus.ERROR);
            return;
        }

        // Initialize new alignment result for the normalized result
        normalizedPeakList = new SimplePeakList(originalPeakList + " " + suffix,
                originalPeakList.getRawDataFiles());

        // Copy raw data files from original alignment result to new alignment
        // result
        totalRows = originalPeakList.getNumberOfRows();

        // Loop through all rows
        rowIteration: for (PeakListRow row : originalPeakList.getRows()) {

            // Cancel ?
            if (isCanceled()) {
                return;
            }

            // Do not add the standard rows to the new peaklist
            for (int i = 0; i < standardRows.length; i++) {
                if (row == standardRows[i]) {
                    processedRows++;
                    continue rowIteration;
                }
            }

            // Copy comment and identification
            SimplePeakListRow normalizedRow = new SimplePeakListRow(
                    row.getID());
            PeakUtils.copyPeakListRowProperties(row, normalizedRow);

            // Get m/z and RT of the current row
            double mz = row.getAverageMZ();
            double rt = row.getAverageRT();

            // Loop through all raw data files
            for (RawDataFile file : originalPeakList.getRawDataFiles()) {

                double normalizationFactors[] = null;
                double normalizationFactorWeights[] = null;

                if (normalizationType == StandardUsageType.Nearest) {

                    // Search for nearest standard
                    PeakListRow nearestStandardRow = null;
                    double nearestStandardRowDistance = Double.MAX_VALUE;

                    for (int standardRowIndex = 0; standardRowIndex < standardRows.length; standardRowIndex++) {
                        PeakListRow standardRow = standardRows[standardRowIndex];

                        double stdMZ = standardRow.getAverageMZ();
                        double stdRT = standardRow.getAverageRT();
                        double distance = MZvsRTBalance * Math.abs(mz - stdMZ)
                                + Math.abs(rt - stdRT);
                        if (distance <= nearestStandardRowDistance) {
                            nearestStandardRow = standardRow;
                            nearestStandardRowDistance = distance;
                        }

                    }

                    assert nearestStandardRow != null;

                    // Calc and store a single normalization factor
                    normalizationFactors = new double[1];
                    normalizationFactorWeights = new double[1];
                    Feature standardPeak = nearestStandardRow.getPeak(file);
                    if (standardPeak == null) {
                        // What to do if standard peak is not available?
                        normalizationFactors[0] = 1.0;
                    } else {
                        if (peakMeasurementType == PeakMeasurementType.HEIGHT) {
                            normalizationFactors[0] = standardPeak.getHeight();
                        } else {
                            normalizationFactors[0] = standardPeak.getArea();
                        }
                    }
                    logger.finest("Normalizing row #" + row.getID()
                            + " using standard peak " + standardPeak
                            + ", factor " + normalizationFactors[0]);
                    normalizationFactorWeights[0] = 1.0f;

                }

                if (normalizationType == StandardUsageType.Weighted) {

                    // Add all standards as factors, and use distance as weight
                    normalizationFactors = new double[standardRows.length];
                    normalizationFactorWeights = new double[standardRows.length];

                    for (int standardRowIndex = 0; standardRowIndex < standardRows.length; standardRowIndex++) {
                        PeakListRow standardRow = standardRows[standardRowIndex];

                        double stdMZ = standardRow.getAverageMZ();
                        double stdRT = standardRow.getAverageRT();
                        double distance = MZvsRTBalance * Math.abs(mz - stdMZ)
                                + Math.abs(rt - stdRT);

                        Feature standardPeak = standardRow.getPeak(file);
                        if (standardPeak == null) {
                            // What to do if standard peak is not available?
                            normalizationFactors[standardRowIndex] = 1.0;
                            normalizationFactorWeights[standardRowIndex] = 0.0;
                        } else {
                            if (peakMeasurementType == PeakMeasurementType.HEIGHT) {
                                normalizationFactors[standardRowIndex] = standardPeak
                                        .getHeight();
                            } else {
                                normalizationFactors[standardRowIndex] = standardPeak
                                        .getArea();
                            }
                            normalizationFactorWeights[standardRowIndex] = 1
                                    / distance;
                        }
                    }

                }

                assert normalizationFactors != null;
                assert normalizationFactorWeights != null;

                // Calculate a single normalization factor as weighted average
                // of all factors
                double weightedSum = 0.0f;
                double sumOfWeights = 0.0f;
                for (int factorIndex = 0; factorIndex < normalizationFactors.length; factorIndex++) {
                    weightedSum += normalizationFactors[factorIndex]
                            * normalizationFactorWeights[factorIndex];
                    sumOfWeights += normalizationFactorWeights[factorIndex];
                }
                double normalizationFactor = weightedSum / sumOfWeights;

                // For simple scaling of the normalized values
                normalizationFactor = normalizationFactor / 100.0f;

                logger.finest("Normalizing row #" + row.getID() + "[" + file
                        + "] using factor " + normalizationFactor);

                // How to handle zero normalization factor?
                if (normalizationFactor == 0.0)
                    normalizationFactor = Double.MIN_VALUE;

                // Normalize peak
                Feature originalPeak = row.getPeak(file);
                if (originalPeak != null) {

                    SimpleFeature normalizedPeak = new SimpleFeature(
                            originalPeak);

                    PeakUtils.copyPeakProperties(originalPeak, normalizedPeak);

                    double normalizedHeight = originalPeak.getHeight()
                            / normalizationFactor;
                    double normalizedArea = originalPeak.getArea()
                            / normalizationFactor;
                    normalizedPeak.setHeight(normalizedHeight);
                    normalizedPeak.setArea(normalizedArea);

                    normalizedRow.addPeak(file, normalizedPeak);
                }

            }

            normalizedPeakList.addRow(normalizedRow);
            processedRows++;

        }

        // Add new peaklist to the project
        project.addPeakList(normalizedPeakList);

        // Load previous applied methods
        for (PeakListAppliedMethod proc : originalPeakList
                .getAppliedMethods()) {
            normalizedPeakList.addDescriptionOfAppliedTask(proc);
        }

        // Add task description to peakList
        normalizedPeakList
                .addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
                        "Standard compound normalization", parameters));

        // Remove the original peaklist if requested
        if (removeOriginal)
            project.removePeakList(originalPeakList);

        logger.info("Finished standard compound normalizer");
        setStatus(TaskStatus.FINISHED);

    }

}
