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

package io.github.mzmine.util;

import com.google.common.collect.Range;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides utility methods to deconvolve sets of x-y data.
 */
public class DeconvolutionUtils {

  /**
   * Basically a general version of local minimum search.
   *
   * @param x                             domain values of the data to be deconvoluted
   * @param y                             range values of the data to be deconvoluted. Has to be
   *                                      strictly monotonically increasing (e.g. RT or mobility).
   *                                      The values inside this array are set to 0 if they fall
   *                                      below the chromatographicThresholdLevel.
   * @param indices                       indices of the data to be deconvoluted in it's original
   *                                      value collection. (has to be strictly monotonically
   *                                      increasing)
   * @param xRange                        {@link io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchPeakDetectorParameters#PEAK_DURATION}
   * @param searchXRange                  {@link io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchPeakDetectorParameters#SEARCH_RT_RANGE}
   * @param minRatio                      {@link io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchPeakDetectorParameters#MIN_RATIO}
   * @param minHeight                     Minimum height to be recognised as a peak.
   * @param chromatographicThresholdLevel {@link io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchPeakDetectorParameters#CHROMATOGRAPHIC_THRESHOLD_LEVEL}
   * @return Collection of a Set of indices for each resolved peak.
   * @see io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchPeakDetector
   */
  public static Set<Set<Integer>> resolveXYDataByLocalMinimum(final double[] x, final double[] y,
      final int[] indices, Range<Double> xRange, final float searchXRange, final double minRatio,
      final double minHeight, final double chromatographicThresholdLevel) {

    if (x.length != y.length || x.length != indices.length) {
      throw new AssertionError("Length of x, y and indices array does not match.");
    }

    // Important: empty scans need to be represented by a 0!
    final int valueCount = x.length;

    Set<Set<Integer>> resolved = new HashSet<>();

    final int lastScan = valueCount - 1;
    assert valueCount > 0;

    // First, remove all data points below chromatographic threshold.
    for (int i = 0; i < y.length; i++) {
      if (y[i] < chromatographicThresholdLevel) {
        y[i] = 0.0;
      }
    }

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

          // Check the shape of the peak.
          if (currentRegionHeight >= minHeight && currentRegionHeight >= peakMinLeft * minRatio
              && currentRegionHeight >= peakMinRight * minRatio && xRange.contains(
              x[currentRegionEnd] - x[currentRegionStart])) {

            int finalCurrentRegionStart = currentRegionStart;
            int finalCurrentRegionEnd = currentRegionEnd;

            Set<Integer> set = new HashSet<>();
            for (Integer id : indices) {
              if (id.intValue() >= finalCurrentRegionStart && id <= finalCurrentRegionEnd) {
                set.add(id);
              }
            }
            resolved.add(set);
          }

          // Set the next region start to current region end - 1
          // because it will be immediately
          // increased +1 as we continue the for-cycle.
          currentRegionStart = currentRegionEnd - 1;
          continue startSearch;
        }

        // Minimum duration of peak must be at least searchXRange.
        if (x[currentRegionEnd]
            - x[currentRegionStart] >= searchXRange) {

          // Set the RT range to check
          final Range<Double> checkRange =
              Range.closed(x[currentRegionEnd] - searchXRange,
                  x[currentRegionEnd] + searchXRange);

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

            // Check the shape of the peak.
            if (currentRegionHeight >= minHeight && currentRegionHeight >= peakMinLeft * minRatio
                && currentRegionHeight >= peakMinRight * minRatio && xRange.contains(
                x[currentRegionEnd] - x[currentRegionStart])) {

              int finalCurrentRegionStart = currentRegionStart;
              int finalCurrentRegionEnd = currentRegionEnd;

              Set<Integer> set = new HashSet<>();
              for (Integer id : indices) {
                if (id.intValue() >= finalCurrentRegionStart
                    && id.intValue() <= finalCurrentRegionEnd) {
                  set.add(id);
                }
              }
              resolved.add(set);
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
