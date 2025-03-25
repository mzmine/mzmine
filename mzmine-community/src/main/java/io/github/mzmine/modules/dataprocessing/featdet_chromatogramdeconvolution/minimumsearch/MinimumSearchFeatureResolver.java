/*
 * Copyright (c) 2004-2024 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.AbstractResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.util.MathUtils;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * This peak recognition method searches for local minima in the chromatogram. If a local minimum is
 * a local minimum even at a given retention time range, it is considered a border between two
 * peaks.
 */
public class MinimumSearchFeatureResolver extends AbstractResolver {

  final Range<Double> xRange;
  final double searchXWidth;
  final double minRatio;
  private final ParameterSet parameters;
  private final double chromThreshold;
  private final int minDataPoints;
  private final double minAbsoluteHeight;
  private final double minRelativeHeight;
  double[] xBuffer;
  double[] yBuffer;

  public MinimumSearchFeatureResolver(ParameterSet parameterSet, ModularFeatureList flist) {
    super(parameterSet, flist);
    this.parameters = parameterSet;
    chromThreshold = parameters.getParameter(CHROMATOGRAPHIC_THRESHOLD_LEVEL).getValue();
    minDataPoints = parameters.getParameter(MIN_NUMBER_OF_DATAPOINTS).getValue();
    xRange = parameters.getParameter(PEAK_DURATION).getValue();
    searchXWidth = parameters.getParameter(SEARCH_RT_RANGE).getValue();
    minRatio = parameters.getParameter(MIN_RATIO).getValue();
    minAbsoluteHeight = parameters.getParameter(MIN_ABSOLUTE_HEIGHT).getValue();
    minRelativeHeight = parameters.getParameter(MIN_RELATIVE_HEIGHT).getValue();
  }

  /**
   * Creates a resolver meant for internal use in other modules. MS2 grouping is disabled by
   * default.
   */
  public MinimumSearchFeatureResolver(ModularFeatureList flist, ResolvingDimension dimension,
      double chromThreshold, double searchRange, double minRelativeHeight, double minAbsoluteHeight,
      double minRatio, Range<Double> duration, int minDataPoints) {
    final ParameterSet resolverParam = ConfigService.getConfiguration()
        .getModuleParameters(MinimumSearchFeatureResolverModule.class).cloneParameterSet();
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.PEAK_LISTS,
        new FeatureListsSelection(flist));
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters, false);

    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.dimension, dimension);
    resolverParam.setParameter(
        MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL, chromThreshold);
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.SEARCH_RT_RANGE, searchRange);
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.MIN_RELATIVE_HEIGHT,
        minRelativeHeight);
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.MIN_ABSOLUTE_HEIGHT,
        minAbsoluteHeight);
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.MIN_RATIO, minRatio);
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.PEAK_DURATION, duration);
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.MIN_NUMBER_OF_DATAPOINTS,
        minDataPoints);

    this(resolverParam, flist);
  }

  @Override
  public Class<? extends MZmineProcessingModule> getModuleClass() {
    return MinimumSearchFeatureResolverModule.class;
  }

  /**
   * @param x domain values of the data to be resolved
   * @param y range values of the data to be resolved. Values have to be <b>strictly monotonically
   *          increasing</b> (e.g. RT or mobility). The values inside this array are set to 0 if
   *          they fall below the chromatographicThresholdLevel.
   * @return List of x values for each resolved peak
   */
  @Override
  @NotNull
  public List<Range<Double>> resolve(double[] x, double[] y) {
    if (x.length != y.length) {
      throw new AssertionError("Length of x, y and indices array does not match.");
    }

    // Important: empty scans need to be represented by a 0!
    final int valueCount = x.length;

    List<Range<Double>> resolved = new ArrayList<>();

    final int lastScan = valueCount - 1;
    assert valueCount > 0;

    // First, remove all data points below chromatographic threshold.
    final double chromatographicThresholdLevel = MathUtils.calcQuantile(y, chromThreshold);
    double maxY = 0;
    for (int i = 0; i < y.length; i++) {
      if (y[i] < chromatographicThresholdLevel) {
        y[i] = 0.0;
      }
      if (y[i] > maxY) {
        maxY = y[i];
      }
    }

    final double minHeight = Math.max(minAbsoluteHeight, minRelativeHeight * maxY);

    // Current region is a region between two minima, representing a
    // candidate for a resolved peak.
    startSearch:
    for (int currentRegionStart = 0; currentRegionStart < lastScan - 2; currentRegionStart++) {

      // Find at least two consecutive non-zero data points
      if (y[currentRegionStart] == 0.0 || y[currentRegionStart + 1] == 0.0) {
        continue;
      }

      double currentRegionHeight = y[currentRegionStart];

      endSearch:
      for (int currentRegionEnd = currentRegionStart + 1; currentRegionEnd < valueCount;
          currentRegionEnd++) {

        // Update height of current region.
        currentRegionHeight = Math.max(currentRegionHeight, y[currentRegionEnd]);

        // If we reached the end, or if the next intensity is 0, we
        // have to stop here.
        if (currentRegionEnd == lastScan || y[currentRegionEnd + 1] == 0.0) {

          // Find the intensity at the sides (lowest data points).
          final double peakMinLeft = y[currentRegionStart];
          final double peakMinRight = y[currentRegionEnd];

          currentRegionStart = tryToFinalizePeak(x, y, currentRegionEnd, currentRegionStart,
              currentRegionHeight, minHeight, peakMinLeft, peakMinRight, resolved);
          continue startSearch;
        }

        // Minimum duration of peak must be at least searchXRange.
        if (x[currentRegionEnd] - x[currentRegionStart] >= searchXWidth) {

          // Set the RT range to check
          final Range<Double> checkRange = Range.closed(x[currentRegionEnd] - searchXWidth,
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
            currentRegionStart = tryToFinalizePeak(x, y, currentRegionEnd, currentRegionStart,
                currentRegionHeight, minHeight, peakMinLeft, peakMinRight, resolved);
            continue startSearch;
          }
        }
      }
    }
    return resolved;
  }

  /**
   * @return The index at which to start to search for the next peak
   */
  private int tryToFinalizePeak(double[] x, double[] y, int currentRegionEnd,
      int currentRegionStart, double currentRegionHeight, double minHeight, double peakMinLeft,
      double peakMinRight, List<Range<Double>> resolved) {
    // inclusive start and end values
    final int numberOfDataPoints = currentRegionEnd - currentRegionStart + 1;
    // Check the shape of the peak.
    if (checkPeakShape(x, numberOfDataPoints, currentRegionHeight, minHeight, peakMinLeft,
        peakMinRight, currentRegionEnd, currentRegionStart)) {

      final Range<Double> range = adjustStartAndEnd(x, y, currentRegionStart, currentRegionEnd);

      resolved.add(range);
    }

    // Set the next region start to current region end-1
    // because it will be immediately
    // increased +1 as we continue the for-cycle.
    currentRegionStart = currentRegionEnd - 1;
    return currentRegionStart;
  }

  private boolean checkPeakShape(double[] x, int numberOfDataPoints, double currentRegionHeight,
      double minHeight, double peakMinLeft, double peakMinRight, int currentRegionEnd,
      int currentRegionStart) {
    return numberOfDataPoints >= minDataPoints && currentRegionHeight >= minHeight
        && currentRegionHeight >= peakMinLeft * minRatio
        && currentRegionHeight >= peakMinRight * minRatio && xRange.contains(
        x[currentRegionEnd] - x[currentRegionStart]);
  }

  /**
   * @return Checks if the current start and end points are non-zero but have zero values next to
   * them. If yes, these zeros will be included in the region of the peak.
   */
  private static @NotNull Range<Double> adjustStartAndEnd(double[] x, double[] y,
      int currentRegionStart, int currentRegionEnd) {
    // adjust start and end points of signals to produce better peak shapes
    // (e.g. include 0 intensity points on the edges.), as start and end points of this
    // resolver will never be 0.
    int start = currentRegionStart;
    if (y[currentRegionStart] != 0 && y[Math.max(currentRegionStart - 1, 0)] == 0.0) {
      start = currentRegionStart - 1;
    }

    int end = currentRegionEnd;
    if (y[currentRegionEnd] != 0 && y[Math.min(currentRegionEnd + 1, y.length - 1)] == 0.0) {
      end = currentRegionEnd + 1;
    }
    final Range<Double> range = Range.closed(x[start], x[end]);
    return range;
  }
}
