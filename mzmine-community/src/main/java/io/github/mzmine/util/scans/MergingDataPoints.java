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

package io.github.mzmine.util.scans;

import io.github.mzmine.util.DataPointSorter;
import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import org.jetbrains.annotations.NotNull;

/**
 * All data points of the spectra that are merged in
 * {@link SpectraMerging#calculatedMergedMzsAndIntensities}, flattened into primitive arrays. The
 * arrays may be longer than {@link #size()}.
 *
 * @param mzs             the m/z values
 * @param intensities     the intensities
 * @param spectrumIndices the index of the source spectrum of each data point
 * @param size            the number of data points
 * @param numSpectra      the number of source spectra
 */
record MergingDataPoints(@NotNull double[] mzs, @NotNull double[] intensities,
                         @NotNull int[] spectrumIndices, int size, int numSpectra) {

  /**
   * @return data point indices sorted by descending intensity, ties by descending m/z. Same order
   * as the {@link DataPointSorter} used in the legacy implementation.
   */
  @NotNull int[] intensityOrderDescending() {
    final int[] order = new int[size];
    for (int i = 0; i < size; i++) {
      order[i] = i;
    }
    // decision: radix sort is much faster than a comparator based sort. Unstable is fine, data points
    // that tie in intensity and m/z are identical.
    DoubleArrays.radixSortIndirect(order, intensities, mzs, 0, size, false);
    // ascending by intensity and m/z -> reverse for descending
    for (int i = 0, j = size - 1; i < j; i++, j--) {
      final int tmp = order[i];
      order[i] = order[j];
      order[j] = tmp;
    }
    return order;
  }
}
