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

package net.sf.mzmine.modules.normalization.linear;

import java.util.Hashtable;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.data.impl.SimplePeak;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.taskcontrol.Task;

public class LinearNormalizerTask implements Task {

    static final double maximumOverallPeakHeightAfterNormalization = 100000.0;

    private PeakList originalPeakList;
    private String normalizationTypeString;

    private TaskStatus status;
    private String errorMessage;

    private int processedDataFiles;
    private int totalDataFiles;

    private SimplePeakList normalizedPeakList;

    public LinearNormalizerTask(PeakList alignmentResult,
            SimpleParameterSet parameters) {
        this.originalPeakList = alignmentResult;

        normalizationTypeString = (String) parameters.getParameterValue(LinearNormalizer.normalizationType);

    }

    public void cancel() {
        status = TaskStatus.CANCELED;

    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public float getFinishedPercentage() {
        return (float) processedDataFiles / (float) totalDataFiles;
    }

    public Object getResult() {
        return normalizedPeakList;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getTaskDescription() {
        return "Linear normalization of " + originalPeakList + " by "
                + normalizationTypeString;
    }

    public void run() {

        status = TaskStatus.PROCESSING;

        totalDataFiles = originalPeakList.getNumberOfRawDataFiles();

        // This hashtable maps rows from original alignment result to rows of
        // the normalized alignment
        Hashtable<PeakListRow, SimplePeakListRow> rowMap = new Hashtable<PeakListRow, SimplePeakListRow>();

        // Initialize new alignment result for to normalized result
        normalizedPeakList = new SimplePeakList(
                "Result from Linear Normalization by "
                        + normalizationTypeString);

        // Copy raw data files from original alignment result to new alignment
        // result
        for (OpenedRawDataFile ord : originalPeakList.getRawDataFiles())
            normalizedPeakList.addOpenedRawDataFile(ord);

        // Loop through all raw data files, and find the peak with biggest
        // height
        double maxOriginalHeight = 0.0;
        for (OpenedRawDataFile ord : originalPeakList.getRawDataFiles()) {
            for (PeakListRow originalAlignmentRow : originalPeakList.getRows()) {
                Peak p = originalAlignmentRow.getPeak(ord);
                if (p != null)
                    if (maxOriginalHeight <= p.getHeight())
                        maxOriginalHeight = p.getHeight();
            }
        }

        // Loop through all raw data files, and normalize peak values
        for (OpenedRawDataFile ord : originalPeakList.getRawDataFiles()) {

            if (status == TaskStatus.CANCELED) {
                normalizedPeakList = null;
                rowMap.clear();
                rowMap = null;
                return;
            }

            // Determine normalization type and calculate normalization factor
            // accordingly
            double normalizationFactor = 1.0;

            // - normalization by average squared peak intensity
            if (normalizationTypeString == LinearNormalizer.NormalizationTypeAverageIntensity) {
                double intensitySum = 0.0;
                int intensityCount = 0;
                for (PeakListRow alignmentRow : originalPeakList.getRows()) {
                    Peak p = alignmentRow.getPeak(ord);
                    if (p != null) {
                        // TODO: Use global parameter to determine whether to
                        // use height or area
                        intensitySum += p.getHeight();
                        intensityCount++;
                    }
                }
                normalizationFactor = intensitySum / (double) intensityCount;
            }

            // - normalization by average squared peak intensity
            if (normalizationTypeString == LinearNormalizer.NormalizationTypeAverageSquaredIntensity) {
                double intensitySum = 0.0;
                int intensityCount = 0;
                for (PeakListRow alignmentRow : originalPeakList.getRows()) {
                    Peak p = alignmentRow.getPeak(ord);
                    if (p != null) {
                        // TODO: Use global parameter to determine whether to
                        // use height or area
                        intensitySum += (p.getHeight() * p.getHeight());
                        intensityCount++;
                    }
                }
                normalizationFactor = intensitySum / (double) intensityCount;
            }

            // - normalization by maximum peak intensity
            if (normalizationTypeString == LinearNormalizer.NormalizationTypeMaximumPeakHeight) {
                double maximumIntensity = 0.0;
                for (PeakListRow alignmentRow : originalPeakList.getRows()) {
                    Peak p = alignmentRow.getPeak(ord);
                    if (p != null) {
                        // TODO: Use global parameter to determine whether to
                        // use height or area
                        if (maximumIntensity < p.getHeight())
                            maximumIntensity = p.getHeight();
                    }
                }
                normalizationFactor = maximumIntensity;
            }

            // - normalization by total raw signal
            if (normalizationTypeString == LinearNormalizer.NormalizationTypeTotalRawSignal) {
                normalizationFactor = ord.getCurrentFile().getDataTotalRawSignal(
                        1);
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
            for (PeakListRow originalAlignmentRow : originalPeakList.getRows()) {
                Peak originalPeak = originalAlignmentRow.getPeak(ord);
                if (originalPeak != null) {
                    SimplePeak normalizedPeak = new SimplePeak(originalPeak);
                    double normalizedHeight = originalPeak.getHeight()
                            / normalizationFactor;
                    double normalizedArea = originalPeak.getArea()
                            / normalizationFactor;
                    normalizedPeak.setHeight(normalizedHeight);
                    normalizedPeak.setArea(normalizedArea);

                    SimplePeakListRow normalizedRow = rowMap.get(originalAlignmentRow);
                    if (normalizedRow == null) {
                        normalizedRow = new SimplePeakListRow(originalAlignmentRow.getID());
                        rowMap.put(originalAlignmentRow, normalizedRow);
                    }

                    normalizedRow.addPeak(ord, originalPeak, normalizedPeak);

                }

            }

            // Progress
            processedDataFiles++;

        }

        // Finally add all normalized rows to normalized alignment result
        for (PeakListRow originalAlignmentRow : originalPeakList.getRows()) {
            SimplePeakListRow normalizedRow = rowMap.get(originalAlignmentRow);
            normalizedPeakList.addRow(normalizedRow);
        }

        status = TaskStatus.FINISHED;
    }

}
