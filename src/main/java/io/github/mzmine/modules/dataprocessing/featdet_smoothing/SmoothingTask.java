/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

/*
 * Code created was by or on behalf of Syngenta and is released under the open source license in use
 * for the pre-existing code or project. Syngenta does not assert ownership or copyright any over
 * pre-existing work.
 */

package io.github.mzmine.modules.dataprocessing.featdet_smoothing;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Range;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleFeature;
import io.github.mzmine.datamodel.impl.SimplePeakList;
import io.github.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimplePeakListRow;
import io.github.mzmine.modules.tools.qualityparameters.QualityParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;

/**
 * Performs chromatographic smoothing of a peak-list.
 *
 */
public class SmoothingTask extends AbstractTask {

    // Logger.
    private static final Logger LOG = Logger
            .getLogger(SmoothingTask.class.getName());

    // Feature lists: original and processed.
    private final MZmineProject project;
    private final PeakList origPeakList;
    private SimplePeakList newPeakList;

    // Parameters.
    private final ParameterSet parameters;
    private final String suffix;
    private final boolean removeOriginal;
    private final int filterWidth;

    private int progress;
    private final int progressMax;

    /**
     * Create the task.
     *
     * @param peakList
     *            the peak-list.
     * @param smoothingParameters
     *            smoothing parameters.
     */
    public SmoothingTask(final MZmineProject project, final PeakList peakList,
            final ParameterSet smoothingParameters) {

        // Initialize.
        this.project = project;
        origPeakList = peakList;
        progress = 0;
        progressMax = peakList.getNumberOfRows();

        // Parameters.
        parameters = smoothingParameters;
        suffix = parameters.getParameter(SmoothingParameters.SUFFIX).getValue();
        removeOriginal = parameters
                .getParameter(SmoothingParameters.REMOVE_ORIGINAL).getValue();
        filterWidth = parameters.getParameter(SmoothingParameters.FILTER_WIDTH)
                .getValue();
    }

    @Override
    public String getTaskDescription() {
        return "Smoothing " + origPeakList;
    }

    @Override
    public double getFinishedPercentage() {
        return progressMax == 0 ? 0.0
                : (double) progress / (double) progressMax;
    }

    @Override
    public void run() {

        setStatus(TaskStatus.PROCESSING);

        try {
            // Get filter weights.
            final double[] filterWeights = SavitzkyGolayFilter
                    .getNormalizedWeights(filterWidth);

            // Create new feature list
            newPeakList = new SimplePeakList(origPeakList + " " + suffix,
                    origPeakList.getRawDataFiles());

            // Process each row.
            for (final PeakListRow row : origPeakList.getRows()) {

                if (!isCanceled()) {

                    // Create a new peak-list row.
                    final int originalID = row.getID();
                    final PeakListRow newRow = new SimplePeakListRow(
                            originalID);

                    // Process each peak.
                    for (final Feature peak : row.getPeaks()) {

                        if (!isCanceled()) {

                            // Copy original peak intensities.
                            final int[] scanNumbers = peak.getScanNumbers();
                            final int numScans = scanNumbers.length;
                            final double[] intensities = new double[numScans];
                            for (int i = 0; i < numScans; i++) {

                                final DataPoint dataPoint = peak
                                        .getDataPoint(scanNumbers[i]);
                                intensities[i] = dataPoint == null ? 0.0
                                        : dataPoint.getIntensity();
                            }

                            // Smooth peak.
                            final double[] smoothed = convolve(intensities,
                                    filterWeights);

                            // Measure peak (max, ranges, area etc.)
                            final RawDataFile dataFile = peak.getDataFile();
                            final DataPoint[] newDataPoints = new DataPoint[numScans];
                            double maxIntensity = 0.0;
                            int maxScanNumber = -1;
                            DataPoint maxDataPoint = null;
                            Range<Double> intensityRange = null;
                            double area = 0.0;
                            for (int i = 0; i < numScans; i++) {

                                final int scanNumber = scanNumbers[i];
                                final DataPoint dataPoint = peak
                                        .getDataPoint(scanNumber);
                                final double intensity = smoothed[i];
                                if (dataPoint != null && intensity > 0.0) {

                                    // Create a new data point.
                                    final double mz = dataPoint.getMZ();
                                    final double rt = dataFile
                                            .getScan(scanNumber)
                                            .getRetentionTime();
                                    final DataPoint newDataPoint = new SimpleDataPoint(
                                            mz, intensity);
                                    newDataPoints[i] = newDataPoint;

                                    // Track maximum intensity data point.
                                    if (intensity > maxIntensity) {

                                        maxIntensity = intensity;
                                        maxScanNumber = scanNumber;
                                        maxDataPoint = newDataPoint;
                                    }

                                    // Update ranges.
                                    if (intensityRange == null) {
                                        intensityRange = Range
                                                .singleton(intensity);
                                    } else {
                                        intensityRange = intensityRange.span(
                                                Range.singleton(intensity));
                                    }

                                    // Accumulate peak area.
                                    if (i != 0) {

                                        final DataPoint lastDP = newDataPoints[i
                                                - 1];
                                        final double lastIntensity = lastDP == null
                                                ? 0.0
                                                : lastDP.getIntensity();
                                        final double lastRT = dataFile
                                                .getScan(scanNumbers[i - 1])
                                                .getRetentionTime();
                                        area += (rt - lastRT) * 60d
                                                * (intensity + lastIntensity)
                                                / 2.0;
                                    }
                                }
                            }

                            assert maxDataPoint != null;

                            if (!isCanceled() && maxScanNumber >= 0) {

                                // Create a new peak.
                                newRow.addPeak(dataFile, new SimpleFeature(
                                        dataFile, maxDataPoint.getMZ(),
                                        peak.getRT(), maxIntensity, area,
                                        scanNumbers, newDataPoints,
                                        peak.getFeatureStatus(), maxScanNumber,
                                        peak.getMostIntenseFragmentScanNumber(),
                                        peak.getAllMS2FragmentScanNumbers(),
                                        peak.getRawDataPointsRTRange(),
                                        peak.getRawDataPointsMZRange(),
                                        intensityRange));
                            }
                        }
                    }
                    newPeakList.addRow(newRow);
                    progress++;
                }
            }

            // Finish up.
            if (!isCanceled()) {

                // Add new peak-list to the project.
                project.addPeakList(newPeakList);

                // Add quality parameters to peaks
                QualityParameters.calculateQualityParameters(newPeakList);

                // Remove the original peak-list if requested.
                if (removeOriginal) {
                    project.removePeakList(origPeakList);
                }

                // Copy previously applied methods
                for (final PeakListAppliedMethod method : origPeakList
                        .getAppliedMethods()) {

                    newPeakList.addDescriptionOfAppliedTask(method);
                }

                // Add task description to peak-list.
                newPeakList.addDescriptionOfAppliedTask(
                        new SimplePeakListAppliedMethod(
                                "Peaks smoothed by Savitzky-Golay filter",
                                parameters));

                LOG.finest("Finished peak smoothing: " + progress
                        + " rows processed");

                setStatus(TaskStatus.FINISHED);
            }
        } catch (Throwable t) {

            LOG.log(Level.SEVERE, "Smoothing error", t);
            setErrorMessage(t.getMessage());
            setStatus(TaskStatus.ERROR);
        }
    }

    /**
     * Convolve a set of weights with a set of intensities.
     *
     * @param intensities
     *            the intensities.
     * @param weights
     *            the filter weights.
     * @return the convolution results.
     */
    private static double[] convolve(final double[] intensities,
            final double[] weights) {

        // Initialise.
        final int fullWidth = weights.length;
        final int halfWidth = (fullWidth - 1) / 2;
        final int numPoints = intensities.length;

        // Convolve.
        final double[] convolved = new double[numPoints];
        for (int i = 0; i < numPoints; i++) {

            double sum = 0.0;
            final int k = i - halfWidth;
            for (int j = Math.max(0, -k); j < Math.min(fullWidth,
                    numPoints - k); j++) {

                sum += intensities[k + j] * weights[j];
            }

            // Set the result.
            convolved[i] = sum;
        }

        return convolved;
    }
}
