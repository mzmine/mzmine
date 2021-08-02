/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch;

import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters.MIN_ABSOLUTE_HEIGHT;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters.MIN_NUMBER_OF_DATAPOINTS;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters.MIN_RATIO;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters.MIN_RELATIVE_HEIGHT;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters.PEAK_DURATION;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters.SEARCH_RT_RANGE;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvedPeak;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvedValue;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.XYResolver;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.R.REngineType;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.maths.CenterFunction;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * This peak recognition method searches for local minima in the chromatogram. If a local minimum is
 * a local minimum even at a given retention time range, it is considered a border between two
 * peaks.
 */
public class MinimumSearchFeatureResolver implements FeatureResolver,
    XYResolver<Double, Double, double[], double[]> {

  private final ParameterSet parameters;
  private final double chromThreshold;
  private final int minDataPoints;
  final Range<Double> xRange;
  final double searchXWidth;
  final double minRatio;

  @Override
  public Class<? extends MZmineProcessingModule> getModuleClass() {
    return MinimumSearchFeatureResolverModule.class;
  }

  public MinimumSearchFeatureResolver(ParameterSet parameterSet) {
    this.parameters = parameterSet;
    chromThreshold = parameters.getParameter(CHROMATOGRAPHIC_THRESHOLD_LEVEL).getValue();
    minDataPoints = parameters.getParameter(MIN_NUMBER_OF_DATAPOINTS).getValue();
    xRange = parameters.getParameter(PEAK_DURATION).getValue();
    searchXWidth = parameters.getParameter(SEARCH_RT_RANGE).getValue();
    minRatio = parameters.getParameter(MIN_RATIO).getValue();
  }

  @Override
  public @NotNull
  String getName() {
    return "Local minimum search";
  }

  @Override
  public ResolvedPeak[] resolvePeaks(final Feature chromatogram, ParameterSet parameters,
      RSessionWrapper rSession, CenterFunction mzCenterFunction, double msmsRange,
      float rTRangeMSMS) {
    throw new UnsupportedOperationException(
        "Legacy resolvePeaks not supported in minimum resolver. use resolve");
  }

  @Override
  public @NotNull
  Class<? extends ParameterSet> getParameterSetClass() {
    return MinimumSearchFeatureResolverParameters.class;
  }

  @Override
  public boolean getRequiresR() {
    return false;
  }

  @Override
  public String[] getRequiredRPackages() {
    return null;
  }

  @Override
  public String[] getRequiredRPackagesVersions() {
    return null;
  }

  @Override
  public REngineType getREngineType(ParameterSet parameters) {
    return null;
  }

  /**
   * @param x domain values of the data to be resolved
   * @param y range values of the data to be resolved. Values have to be <b>strictly monotonically
   *          increasing</b> (e.g. RT or mobility). The values inside this array are set to 0 if
   *          they fall below the chromatographicThresholdLevel.
   * @return Collection of a Set of x values for each resolved peak
   */
  @Override
  public Set<List<ResolvedValue<Double, Double>>> resolve(double[] x, double[] y, int totalScans) {
    if (x.length != y.length) {
      throw new AssertionError("Length of x, y and indices array does not match.");
    }

    // Important: empty scans need to be represented by a 0!
    final int valueCount = x.length;

    Set<List<ResolvedValue<Double, Double>>> resolved = new LinkedHashSet<>();

    final int lastScan = valueCount - 1;
    assert valueCount > 0;

    // First, remove all data points below chromatographic threshold.
    final double chromatographicThresholdLevel = MathUtils
        .calcQuantile(y, chromThreshold, totalScans);
    double maxY = 0;
    for (int i = 0; i < y.length; i++) {
      if (y[i] < chromatographicThresholdLevel) {
        y[i] = 0.0;
      }
      if (y[i] > maxY) {
        maxY = y[i];
      }
    }
    final double minHeight = Math.max(parameters.getParameter(MIN_ABSOLUTE_HEIGHT).getValue(),
        parameters.getParameter(MIN_RELATIVE_HEIGHT).getValue() * maxY);

    // Current region is a region between two minima, representing a
    // candidate for a resolved peak.
    startSearch:
    for (int currentRegionStart = 0; currentRegionStart < lastScan
        - 2; currentRegionStart++) {

      // Find at least two consecutive non-zero data points
      if (y[currentRegionStart] == 0.0 || y[currentRegionStart + 1] == 0.0) {
        continue;
      }

      double currentRegionHeight = y[currentRegionStart];

      endSearch:
      for (int currentRegionEnd =
          currentRegionStart + 1; currentRegionEnd < valueCount; currentRegionEnd++) {

        // Update height of current region.
        currentRegionHeight = Math.max(currentRegionHeight, y[currentRegionEnd]);

        // If we reached the end, or if the next intensity is 0, we
        // have to stop here.
        if (currentRegionEnd == lastScan || y[currentRegionEnd + 1] == 0.0) {

          // Find the intensity at the sides (lowest data points).
          final double peakMinLeft = y[currentRegionStart];
          final double peakMinRight = y[currentRegionEnd];

          // inclusive start and end values
          final int numberOfDataPoints = currentRegionEnd - currentRegionStart + 1;
          // Check the shape of the peak.
          if (numberOfDataPoints >= minDataPoints &&
              currentRegionHeight >= minHeight && currentRegionHeight >= peakMinLeft * minRatio
              && currentRegionHeight >= peakMinRight * minRatio && xRange.contains(
              x[currentRegionEnd] - x[currentRegionStart])) {

            List<ResolvedValue<Double, Double>> resolvedValues = new ArrayList<>();
            for (int i = currentRegionStart; i <= currentRegionEnd; i++) {
              resolvedValues.add(new ResolvedValue<>(x[i], y[i]));
            }

            resolved.add(resolvedValues);
          }

          // Set the next region start to current region end - 1
          // because it will be immediately
          // increased +1 as we continue the for-cycle.
          currentRegionStart = currentRegionEnd - 1;
          continue startSearch;
        }

        // Minimum duration of peak must be at least searchXRange.
        if (x[currentRegionEnd]
            - x[currentRegionStart] >= searchXWidth) {

          // Set the RT range to check
          final Range<Double> checkRange =
              Range.closed(x[currentRegionEnd] - searchXWidth,
                  x[currentRegionEnd] + searchXWidth);

          // Search if there is lower data point on the left from
          // current peak i.
          for (int i = currentRegionEnd - 1; i > 0; i--) {

            if (!checkRange.contains(x[i])) {
              break;
            }

            if (y[i] < y[currentRegionEnd]) {

              continue endSearch;
            }
          }

          // Search on the right from current peak i.
          for (int i = currentRegionEnd + 1; i < valueCount; i++) {

            if (!checkRange.contains(x[i])) {
              break;
            }

            if (y[i] < y[currentRegionEnd]) {

              continue endSearch;
            }
          }

          // Find the intensity at the sides (lowest data points).
          final double peakMinLeft = y[currentRegionStart];
          final double peakMinRight = y[currentRegionEnd];

          // If we have reached a minimum which is non-zero, but
          // the peak shape would not fulfill the
          // ratio condition, continue searching for next minimum.
          if (currentRegionHeight >= peakMinRight * minRatio) {
            // inclusive start and end values
            final int numberOfDataPoints = currentRegionEnd - currentRegionStart + 1;
            // Check the shape of the peak.
            if (numberOfDataPoints >= minDataPoints && currentRegionHeight >= minHeight
                && currentRegionHeight >= peakMinLeft * minRatio
                && currentRegionHeight >= peakMinRight * minRatio && xRange.contains(
                x[currentRegionEnd] - x[currentRegionStart])) {

              List<ResolvedValue<Double, Double>> resolvedValues = new ArrayList<>();
              for (int i = currentRegionStart; i <= currentRegionEnd; i++) {
                resolvedValues.add(new ResolvedValue<>(x[i], y[i]));
              }
              resolved.add(resolvedValues);
            }

            // Set the next region start to current region end-1
            // because it will be immediately
            // increased +1 as we continue the for-cycle.
            currentRegionStart = currentRegionEnd - 1;
            continue startSearch;
          }
        }
      }
    }
    return resolved;
  }
}
