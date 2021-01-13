/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

/*
 * Code created was by or on behalf of Syngenta and is released under the open source license in use
 * for the pre-existing code or project. Syngenta does not assert ownership or copyright any over
 * pre-existing work.
 */

package io.github.mzmine.modules.dataprocessing.featdet_smoothing;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.RangeUtils;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Performs chromatographic smoothing of a peak-list.
 */
public class SmoothingTask extends AbstractTask {

  // Logger.
  private static final Logger logger = Logger.getLogger(SmoothingTask.class.getName());

  // Feature lists: original and processed.
  private final MZmineProject project;
  private final ModularFeatureList origPeakList;
  private ModularFeatureList newPeakList;

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
   * @param peakList            the peak-list.
   * @param smoothingParameters smoothing parameters.
   */
  public SmoothingTask(final MZmineProject project, final FeatureList peakList,
      final ParameterSet smoothingParameters) {

    // Initialize.
    this.project = project;
    origPeakList = (ModularFeatureList) peakList;
    progress = 0;
    progressMax = peakList.getNumberOfRows();

    // Parameters.
    parameters = smoothingParameters;
    suffix = parameters.getParameter(SmoothingParameters.SUFFIX).getValue();
    removeOriginal = parameters.getParameter(SmoothingParameters.REMOVE_ORIGINAL).getValue();
    filterWidth = parameters.getParameter(SmoothingParameters.FILTER_WIDTH).getValue();
  }

  @Override
  public String getTaskDescription() {
    return "Smoothing " + origPeakList;
  }

  @Override
  public double getFinishedPercentage() {
    return progressMax == 0 ? 0.0 : (double) progress / (double) progressMax;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    try {
      // Get filter weights.
      final double[] filterWeights = SavitzkyGolayFilter.getNormalizedWeights(filterWidth);

      // Create new feature list
      newPeakList = origPeakList.createCopy(origPeakList.getName() + " " + suffix);
      newPeakList = new ModularFeatureList(origPeakList + " " + suffix,
          origPeakList.getRawDataFiles());

      // Process each row.
      for (final FeatureListRow row : origPeakList.getRows()) {

        if (!isCanceled()) {

          // Create a new peak-list row.
          final int originalID = row.getID();
          final ModularFeatureListRow newRow = new ModularFeatureListRow(newPeakList, originalID);

          // Process each peak.
          for (final Feature peak : row.getFeatures()) {
            ModularFeature feature = (ModularFeature) peak;
            // TODO feature data

            if (!isCanceled()) {

              // Copy original peak intensities.
              final List<Scan> scans = (List<Scan>) ((ModularFeatureList) feature.getFeatureList())
                  .getSeletedScans(feature.getRawDataFile());
              final int numScans = scans.size();
              final double[] intensities = new double[numScans];
              for (int i = 0; i < numScans; i++) {
                intensities[i] = feature.getFeatureData().getIntensityForScan(scans.get(i));
              }

              // Smooth peak.
              final double[] smoothed = convolve(intensities, filterWeights);

              // keep track for new feature data
              // we won't add intensities for point's that were 0 before!
              double[] newIntensities = new double[feature.getFeatureData().getNumberOfValues()];
              int newIntensitiesIndex = 0;

              // Measure peak (max, ranges, area etc.)
              final RawDataFile dataFile = feature.getRawDataFile();
              float maxIntensity = 0f;
              Scan maxScan = null;
              int maxIntensityIndex = -1;
              Range<Double> intensityRange = null;
              float area = 0f;

              for (int i = 0; i < numScans; i++) {
                final Scan scan = scans.get(i);
//                final DataPoint dataPoint = peak.getDataPointAtIndex(i);
                final double originalIntensity = feature.getFeatureData().getIntensityForScan(scan);
                final double intensity = smoothed[i] > 0 ? smoothed[i] : 0d;

                if (originalIntensity > 0d) {
                  // Create a new data point.
//                  final double mz = feature.getFeatureData().getMzForScan(scan);
                  final double rt = scan.getRetentionTime();
//                  final DataPoint newDataPoint = new SimpleDataPoint(mz, intensity);
                  newIntensities[newIntensitiesIndex] = intensity;

                  // Track maximum intensity data point.
                  if (intensity > maxIntensity) {
                    maxIntensity = (float) intensity;
                    maxScan = scan;
                    maxIntensityIndex = i;
                  }

                  // Update ranges.
                  if (intensityRange == null) {
                    intensityRange = Range.singleton(intensity);
                  } else {
                    intensityRange = intensityRange.span(Range.singleton(intensity));
                  }

                  // Accumulate peak area.
                  if (i != 0) {
                    final double lastIntensity = smoothed[i-1] < 0 ? 0.0 : smoothed[i];
                    final double lastRT = scans.get(i - 1).getRetentionTime();
                    area += (rt - lastRT) * 60d * (intensity + lastIntensity) / 2.0;
                  }
                }
              }

              assert maxIntensityIndex != -1;

              if (!isCanceled() && maxScan != null) {
                // Create a new peak.
                // TODO
                ModularFeature newFeature
                    = new ModularFeature(newPeakList, dataFile, feature.getMZ(), peak.getRT(),
                    maxIntensity,
                    area, feature.getFeatureData().getSpectra(), newIntensities,  peak.getFeatureStatus(),
                    maxScan,
                    peak.getMostIntenseFragmentScan(),
                    peak.getAllMS2FragmentScans().toArray(Scan[]::new),
                    peak.getRawDataPointsRTRange(),
                    peak.getRawDataPointsMZRange(), RangeUtils.toFloatRange(intensityRange));
                newRow.addFeature(dataFile, newFeature);
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
        project.addFeatureList(newPeakList);

        // Add quality parameters to peaks
        //QualityParameters.calculateQualityParameters(newPeakList);

        // Remove the original peak-list if requested.
        if (removeOriginal) {
          project.removeFeatureList(origPeakList);
        }

        // Copy previously applied methods
        for (final FeatureListAppliedMethod method : origPeakList.getAppliedMethods()) {

          newPeakList.addDescriptionOfAppliedTask(method);
        }

        // Add task description to peak-list.
        newPeakList.addDescriptionOfAppliedTask(
            new SimpleFeatureListAppliedMethod("Peaks smoothed by Savitzky-Golay filter",
                parameters));

        logger.finest("Finished peak smoothing: " + progress + " rows processed");

        setStatus(TaskStatus.FINISHED);
      }
    } catch (Throwable t) {
      t.printStackTrace();
      logger.log(Level.SEVERE, "Smoothing error", t);
      setErrorMessage(t.getMessage());
      setStatus(TaskStatus.ERROR);
    }
  }

  /**
   * Convolve a set of weights with a set of intensities.
   *
   * @param intensities the intensities.
   * @param weights     the filter weights.
   * @return the convolution results.
   */
  private static double[] convolve(final double[] intensities, final double[] weights) {

    // Initialise.
    final int fullWidth = weights.length;
    final int halfWidth = (fullWidth - 1) / 2;
    final int numPoints = intensities.length;

    // Convolve.
    final double[] convolved = new double[numPoints];
    for (int i = 0; i < numPoints; i++) {

      double sum = 0.0;
      final int k = i - halfWidth;
      for (int j = Math.max(0, -k); j < Math.min(fullWidth, numPoints - k); j++) {
        sum += intensities[k + j] * weights[j];
      }

      // Set the result.
      convolved[i] = sum;
    }

    return convolved;
  }
}