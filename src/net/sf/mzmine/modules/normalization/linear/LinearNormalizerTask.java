/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.normalization.linear;

import java.util.Hashtable;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListAppliedMethod;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleChromatographicPeak;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;

class LinearNormalizerTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	public static final double maximumOverallPeakHeightAfterNormalization = 100000.0;

    private PeakList originalPeakList;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    private int processedDataFiles, totalDataFiles;

    private String suffix, normalizationType, peakMeasurementType;
    private boolean removeOriginal;
    private LinearNormalizerParameters parameters;

    public LinearNormalizerTask(PeakList peakList,
            LinearNormalizerParameters parameters) {

        this.originalPeakList = peakList;
        this.parameters = parameters;

        totalDataFiles = originalPeakList.getNumberOfRawDataFiles();

        suffix = (String) parameters.getParameterValue(LinearNormalizerParameters.suffix);
        normalizationType = (String) parameters.getParameterValue(LinearNormalizerParameters.normalizationType);
        peakMeasurementType = (String) parameters.getParameterValue(LinearNormalizerParameters.peakMeasurementType);
        removeOriginal = (Boolean) parameters.getParameterValue(LinearNormalizerParameters.autoRemove);

    }

    public void cancel() {
        status = TaskStatus.CANCELED;

    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public double getFinishedPercentage() {
        return (double) processedDataFiles / (double) totalDataFiles;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getTaskDescription() {
        return "Linear normalization of " + originalPeakList + " by "
                + normalizationType;
    }

    public void run() {

        status = TaskStatus.PROCESSING;
        logger.info("Running linear normalizer");

        // This hashtable maps rows from original alignment result to rows of
        // the normalized alignment
        Hashtable<PeakListRow, SimplePeakListRow> rowMap = new Hashtable<PeakListRow, SimplePeakListRow>();

        // Create new peak list
        SimplePeakList normalizedPeakList = new SimplePeakList(originalPeakList
                + " " + suffix, originalPeakList.getRawDataFiles());

        // Loop through all raw data files, and find the peak with biggest
        // height
        double maxOriginalHeight = 0.0f;
        for (RawDataFile file : originalPeakList.getRawDataFiles()) {
            for (PeakListRow originalpeakListRow : originalPeakList.getRows()) {
                ChromatographicPeak p = originalpeakListRow.getPeak(file);
                if (p != null)
                    if (maxOriginalHeight <= p.getHeight())
                        maxOriginalHeight = p.getHeight();
            }
        }

        // Loop through all raw data files, and normalize peak values
        for (RawDataFile file : originalPeakList.getRawDataFiles()) {

            // Cancel?
            if (status == TaskStatus.CANCELED) {
                return;
            }

            // Determine normalization type and calculate normalization factor
            // accfileingly
            double normalizationFactor = 1.0f;

            // - normalization by average squared peak intensity
            if (normalizationType == LinearNormalizerParameters.NormalizationTypeAverageIntensity) {
                double intensitySum = 0.0f;
                int intensityCount = 0;
                for (PeakListRow peakListRow : originalPeakList.getRows()) {
                    ChromatographicPeak p = peakListRow.getPeak(file);
                    if (p != null) {
                        if (peakMeasurementType == LinearNormalizerParameters.PeakMeasurementTypeHeight) {
                            intensitySum += p.getHeight();
                        } else {
                            intensitySum += p.getArea();
                        }
                        intensityCount++;
                    }
                }
                normalizationFactor = intensitySum / (double) intensityCount;
            }

            // - normalization by average squared peak intensity
            if (normalizationType == LinearNormalizerParameters.NormalizationTypeAverageSquaredIntensity) {
                double intensitySum = 0.0f;
                int intensityCount = 0;
                for (PeakListRow peakListRow : originalPeakList.getRows()) {
                    ChromatographicPeak p = peakListRow.getPeak(file);
                    if (p != null) {
                        if (peakMeasurementType == LinearNormalizerParameters.PeakMeasurementTypeHeight) {
                            intensitySum += (p.getHeight() * p.getHeight());
                        } else {
                            intensitySum += (p.getArea() * p.getArea());
                        }
                        intensityCount++;
                    }
                }
                normalizationFactor = intensitySum / (double) intensityCount;
            }

            // - normalization by maximum peak intensity
            if (normalizationType == LinearNormalizerParameters.NormalizationTypeMaximumPeakHeight) {
                double maximumIntensity = 0.0f;
                for (PeakListRow peakListRow : originalPeakList.getRows()) {
                    ChromatographicPeak p = peakListRow.getPeak(file);
                    if (p != null) {
                        if (peakMeasurementType == LinearNormalizerParameters.PeakMeasurementTypeHeight) {
                            if (maximumIntensity < p.getHeight())
                                maximumIntensity = p.getHeight();
                        } else {
                            if (maximumIntensity < p.getArea())
                                maximumIntensity = p.getArea();
                        }

                    }
                }
                normalizationFactor = maximumIntensity;
            }

            // - normalization by total raw signal
            if (normalizationType == LinearNormalizerParameters.NormalizationTypeTotalRawSignal) {
                normalizationFactor = 0;
                for (int scanNumber : file.getScanNumbers(1)) {
                    Scan scan = file.getScan(scanNumber);
                    normalizationFactor += scan.getTIC();
                }
            }

            // Find peak with maximum height and calculate scaling the brings
            // height of this peak to

            // Readjust normalization factor so that maximum height will be
            // equal to maximumOverallPeakHeightAfterNormalization after
            // normalization
            double maxNormalizedHeight = maxOriginalHeight
                    / normalizationFactor;
            normalizationFactor = normalizationFactor * maxNormalizedHeight
                    / maximumOverallPeakHeightAfterNormalization;

            // Normalize all peak intenisities using the normalization factor
            for (PeakListRow originalpeakListRow : originalPeakList.getRows()) {

                // Cancel?
                if (status == TaskStatus.CANCELED) {
                    return;
                }

                ChromatographicPeak originalPeak = originalpeakListRow.getPeak(file);
                if (originalPeak != null) {
                    SimpleChromatographicPeak normalizedPeak = new SimpleChromatographicPeak(
                            originalPeak);
                    double normalizedHeight = originalPeak.getHeight()
                            / normalizationFactor;
                    double normalizedArea = originalPeak.getArea()
                            / normalizationFactor;
                    normalizedPeak.setHeight(normalizedHeight);
                    normalizedPeak.setArea(normalizedArea);

                    SimplePeakListRow normalizedRow = rowMap.get(originalpeakListRow);
                    if (normalizedRow == null) {
                        normalizedRow = new SimplePeakListRow(
                                originalpeakListRow.getID());
                        normalizedRow.setComment(originalpeakListRow.getComment());
                        for (PeakIdentity ident : originalpeakListRow.getPeakIdentities())
                            normalizedRow.addPeakIdentity(ident, false);
                        rowMap.put(originalpeakListRow, normalizedRow);
                    }

                    normalizedRow.addPeak(file, normalizedPeak);

                }

            }

            // Progress
            processedDataFiles++;

        }

        // Finally add all normalized rows to normalized alignment result
        for (PeakListRow originalpeakListRow : originalPeakList.getRows()) {
            SimplePeakListRow normalizedRow = rowMap.get(originalpeakListRow);
            normalizedPeakList.addRow(normalizedRow);
        }

        // Add new peaklist to the project
        MZmineProject currentProject = MZmineCore.getCurrentProject();
        currentProject.addPeakList(normalizedPeakList);
        
		// Load previous applied methods
		for (PeakListAppliedMethod proc: originalPeakList.getAppliedMethods()){
			normalizedPeakList.addDescriptionOfAppliedTask(proc);
		}
		
        // Add task description to peakList
        normalizedPeakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod("Linear normalization of by "
                + normalizationType, parameters));


        // Remove the original peaklist if requested
        if (removeOriginal)
            currentProject.removePeakList(originalPeakList);

        logger.info("Finished linear normalizer");
        status = TaskStatus.FINISHED;
        
    }

}
