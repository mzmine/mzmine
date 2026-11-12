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

package io.github.mzmine.modules.visualization.chromatogram_comparison;

import it.unimi.dsi.fastutil.ints.IntArrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Finds the chromatogram of a list that contains a data point. Both builders copy the data points
 * of the same mass lists, so a data point is identified by its scan and m/z.
 */
final class DataPointIndex {

  // data points of scan s are in [offsets[s], offsets[s + 1]), sorted by m/z
  private final int @NotNull [] offsets;
  private final double @NotNull [] mzs;
  private final @NotNull ComparedChromatogram @NotNull [] owners;

  DataPointIndex(@NotNull List<ComparedChromatogram> chromatograms, int numScans) {
    offsets = new int[numScans + 1];
    for (final ComparedChromatogram chromatogram : chromatograms) {
      for (final int scan : chromatogram.scans()) {
        offsets[scan + 1]++;
      }
    }
    for (int s = 0; s < numScans; s++) {
      offsets[s + 1] += offsets[s];
    }
    final int total = offsets[numScans];
    final double[] unsortedMzs = new double[total];
    final ComparedChromatogram[] unsortedOwners = new ComparedChromatogram[total];
    final int[] next = offsets.clone();
    for (final ComparedChromatogram chromatogram : chromatograms) {
      for (int i = 0; i < chromatogram.scans().length; i++) {
        final int position = next[chromatogram.scans()[i]]++;
        unsortedMzs[position] = chromatogram.mzs()[i];
        unsortedOwners[position] = chromatogram;
      }
    }
    final int[] order = new int[total];
    for (int i = 0; i < total; i++) {
      order[i] = i;
    }
    for (int s = 0; s < numScans; s++) {
      IntArrays.quickSort(order, offsets[s], offsets[s + 1],
          (a, b) -> Double.compare(unsortedMzs[a], unsortedMzs[b]));
    }
    mzs = new double[total];
    owners = new ComparedChromatogram[total];
    for (int i = 0; i < total; i++) {
      mzs[i] = unsortedMzs[order[i]];
      owners[i] = unsortedOwners[order[i]];
    }
  }

  /**
   * @return the chromatogram with a data point of this m/z in the scan, null if none
   */
  @Nullable ComparedChromatogram find(int scan, double mz) {
    int low = offsets[scan];
    int high = offsets[scan + 1];
    final int end = high;
    while (low < high) {
      final int mid = (low + high) >>> 1;
      if (mzs[mid] < mz) {
        low = mid + 1;
      } else {
        high = mid;
      }
    }
    final double maxDistance = ChromatogramComparison.SAME_DATA_POINT_RELATIVE_MZ * mz;
    if (low < end && mzs[low] - mz <= maxDistance) {
      return owners[low];
    }
    if (low > offsets[scan] && mz - mzs[low - 1] <= maxDistance) {
      return owners[low - 1];
    }
    return null;
  }
}
