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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import org.jetbrains.annotations.NotNull;

/**
 * Lower bound search in a fixed sorted array in constant time. A table of equally wide bins stores
 * the first index of each bin, a query only scans the few values in its bin. Used for the many
 * lookups of loose data points in the channel m/z values.
 */
final class BinnedLowerBound {

  private static final int MAX_BINS = 1 << 20;

  private final double[] values;
  private final double origin;
  private final double binWidth;
  private final int[] binStart;

  /**
   * @param values            sorted ascending
   * @param requestedBinWidth bin width, increased if the value range needs too many bins
   */
  BinnedLowerBound(@NotNull double[] values, double requestedBinWidth) {
    this.values = values;
    if (values.length == 0) {
      origin = 0;
      binWidth = 1;
      binStart = new int[0];
      return;
    }
    origin = values[0];
    final double range = values[values.length - 1] - origin;
    binWidth = Math.max(requestedBinWidth, range / (MAX_BINS - 1));
    final int numBins = (int) (range / binWidth) + 1;
    binStart = new int[numBins];
    int index = 0;
    for (int bin = 0; bin < numBins; bin++) {
      final double start = origin + bin * binWidth;
      while (index < values.length && values[index] < start) {
        index++;
      }
      binStart[bin] = index;
    }
  }

  /**
   * @return first index with values[index] > key, values.length if there is none
   */
  int upperBound(double key) {
    // the next larger double: values >= it are exactly the values > key
    return lowerBound(Math.nextUp(key));
  }

  /**
   * @return first index with values[index] >= key, values.length if there is none
   */
  int lowerBound(double key) {
    if (values.length == 0 || !(key > origin)) {
      return 0;
    }
    final int bin = (int) ((key - origin) / binWidth);
    if (bin >= binStart.length) {
      // behind the last bin, all values may still be smaller
      int i = binStart.length == 0 ? 0 : binStart[binStart.length - 1];
      while (i < values.length && values[i] < key) {
        i++;
      }
      return i;
    }
    int i = binStart[bin];
    // rounding of the bin index may put the key into the next bin
    while (i > 0 && values[i - 1] >= key) {
      i--;
    }
    while (i < values.length && values[i] < key) {
      i++;
    }
    return i;
  }
}
