/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.local_max;

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Corrects the m/z of saturated centroids with the unsaturated flanks of the same ion in the
 * neighbouring mobility scans. Detector saturation distorts only the apex mobility scans of a drift
 * peak, the m/z of the unsaturated scans of the same ion is not affected (Agilent IMS centroiding
 * benchmark).
 * <ol>
 *   <li>Every centroid with an apex height at or above the threshold seeds a group.</li>
 *   <li>The group follows the nearest centroid within the m/z tolerance of the seed into the
 *   neighbouring mobility scans, a single scan without a match is bridged.</li>
 *   <li>m/z = height weighted mean of the members below the threshold and above
 *   {@link #MIN_RELATIVE_HEIGHT} of the highest member.</li>
 *   <li>Only the m/z of the saturated members is replaced. Groups without usable unsaturated
 *   members keep their m/z (e.g., from the EMG fit).</li>
 * </ol>
 */
public final class MobilityFlankConsolidator {

  /**
   * Members below this fraction of the highest member are ignored, their m/z is dominated by
   * counting noise.
   */
  public static final double MIN_RELATIVE_HEIGHT = 0.02;
  /**
   * Number of consecutive mobility scans without a match that are bridged
   */
  // decision: low intensity flank scans may miss a centroid, a single gap does not end the group
  public static final int MAX_GAP_SCANS = 1;

  private MobilityFlankConsolidator() {
  }

  /**
   * @param mzs       centroid m/z of each mobility scan (sorted ascending), in mobility scan order.
   *                  Not changed.
   * @param heights   raw apex height of each centroid, same layout as mzs
   * @param threshold apex height from which on a centroid is saturated
   * @param tolerance m/z tolerance to follow an ion into the neighbouring scans
   * @return corrected m/z per scan; arrays without corrections are the input arrays. A corrected
   * scan stays sorted only if the corrections keep the order, check with {@link #isSorted}.
   */
  public static @NotNull List<double[]> consolidate(@NotNull final List<double[]> mzs,
      @NotNull final List<double[]> heights, final double threshold,
      @NotNull final MZTolerance tolerance) {
    final int numScans = mzs.size();
    final List<double[]> result = new ArrayList<>(mzs);
    if (Double.isInfinite(threshold) || Double.isNaN(threshold)) {
      return result;
    }

    // saturated centroids already part of a group
    final List<boolean[]> assigned = new ArrayList<>(numScans);
    for (final double[] scanMzs : mzs) {
      assigned.add(new boolean[scanMzs.length]);
    }

    for (int scan = 0; scan < numScans; scan++) {
      final double[] scanHeights = heights.get(scan);
      for (int i = 0; i < scanHeights.length; i++) {
        if (scanHeights[i] < threshold || assigned.get(scan)[i]) {
          continue;
        }
        consolidateGroup(mzs, heights, result, assigned, scan, i, threshold, tolerance);
      }
    }
    return result;
  }

  private static void consolidateGroup(@NotNull final List<double[]> mzs,
      @NotNull final List<double[]> heights, @NotNull final List<double[]> result,
      @NotNull final List<boolean[]> assigned, final int seedScan, final int seedIndex,
      final double threshold, @NotNull final MZTolerance tolerance) {
    final double seedMz = mzs.get(seedScan)[seedIndex];
    // member scan and centroid index
    final List<int[]> members = new ArrayList<>();
    members.add(new int[]{seedScan, seedIndex});
    collect(mzs, seedMz, tolerance, seedScan, -1, members);
    collect(mzs, seedMz, tolerance, seedScan, +1, members);

    double maxHeight = 0;
    for (final int[] member : members) {
      maxHeight = Math.max(maxHeight, heights.get(member[0])[member[1]]);
    }
    final double minHeight = MIN_RELATIVE_HEIGHT * maxHeight;

    double sumMzHeight = 0;
    double sumHeight = 0;
    for (final int[] member : members) {
      final double height = heights.get(member[0])[member[1]];
      if (height >= threshold) {
        assigned.get(member[0])[member[1]] = true;
      } else if (height >= minHeight && height > 0) {
        sumMzHeight += mzs.get(member[0])[member[1]] * height;
        sumHeight += height;
      }
    }
    if (sumHeight <= 0) {
      // no usable unsaturated flank, keep the m/z of the saturated members
      return;
    }

    final double flankMz = sumMzHeight / sumHeight;
    for (final int[] member : members) {
      if (heights.get(member[0])[member[1]] < threshold) {
        continue;
      }
      double[] scanResult = result.get(member[0]);
      if (scanResult == mzs.get(member[0])) {
        // copy on first change
        scanResult = Arrays.copyOf(scanResult, scanResult.length);
        result.set(member[0], scanResult);
      }
      scanResult[member[1]] = flankMz;
    }
  }

  /**
   * Follows the ion from the seed scan in one direction and adds the nearest centroid within the
   * tolerance of each scan.
   */
  private static void collect(@NotNull final List<double[]> mzs, final double seedMz,
      @NotNull final MZTolerance tolerance, final int seedScan, final int direction,
      @NotNull final List<int[]> members) {
    int gap = 0;
    for (int scan = seedScan + direction; scan >= 0 && scan < mzs.size(); scan += direction) {
      final int index = nearestWithinTolerance(mzs.get(scan), seedMz, tolerance);
      if (index < 0) {
        if (++gap > MAX_GAP_SCANS) {
          return;
        }
        continue;
      }
      gap = 0;
      members.add(new int[]{scan, index});
    }
  }

  /**
   * @return index of the centroid nearest to mz within the tolerance or -1
   */
  static int nearestWithinTolerance(final double @NotNull [] sortedMzs, final double mz,
      @NotNull final MZTolerance tolerance) {
    int insertion = Arrays.binarySearch(sortedMzs, mz);
    if (insertion >= 0) {
      return insertion;
    }
    insertion = -insertion - 1;
    int best = -1;
    double bestDelta = Double.MAX_VALUE;
    for (int i = Math.max(0, insertion - 1); i <= Math.min(sortedMzs.length - 1, insertion); i++) {
      final double delta = Math.abs(sortedMzs[i] - mz);
      if (delta < bestDelta) {
        bestDelta = delta;
        best = i;
      }
    }
    return best >= 0 && tolerance.checkWithinTolerance(sortedMzs[best], mz) ? best : -1;
  }

  /**
   * @return true if the values are sorted ascending
   */
  public static boolean isSorted(final double @NotNull [] values) {
    for (int i = 1; i < values.length; i++) {
      if (values[i] < values[i - 1]) {
        return false;
      }
    }
    return true;
  }
}
