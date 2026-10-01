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

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Collects the full width at half maximum in scans of the most intense peak of each chromatogram
 * with an apex of at least a min intensity. The width spans the consecutive scans around the apex
 * with at least half its intensity, a scan without data point ends it.
 */
final class PeakWidthCollector implements Consumer<BuiltChromatogram> {

  private final double minApexIntensity;
  private final IntArrayList widths = new IntArrayList();

  /**
   * @param minApexIntensity only peaks of at least this height are measured, clear signals
   */
  PeakWidthCollector(double minApexIntensity) {
    this.minApexIntensity = minApexIntensity;
  }

  @Override
  public void accept(BuiltChromatogram chromatogram) {
    final int n = chromatogram.getNumberOfDataPoints();
    int apex = -1;
    double apexIntensity = 0d;
    for (int i = 0; i < n; i++) {
      if (chromatogram.getIntensity(i) > apexIntensity) {
        apexIntensity = chromatogram.getIntensity(i);
        apex = i;
      }
    }
    if (apex < 0 || apexIntensity < minApexIntensity) {
      return;
    }
    final double half = apexIntensity / 2d;
    int first = apex;
    while (first > 0 && chromatogram.getScanIndex(first - 1) == chromatogram.getScanIndex(first) - 1
        && chromatogram.getIntensity(first - 1) >= half) {
      first--;
    }
    int last = apex;
    while (last + 1 < n && chromatogram.getScanIndex(last + 1) == chromatogram.getScanIndex(last) + 1
        && chromatogram.getIntensity(last + 1) >= half) {
      last++;
    }
    widths.add(chromatogram.getScanIndex(last) - chromatogram.getScanIndex(first) + 1);
  }

  int getNumPeaks() {
    return widths.size();
  }

  /**
   * @return the width at the quantile, NaN without peaks
   */
  double quantile(double quantile) {
    if (widths.isEmpty()) {
      return Double.NaN;
    }
    final int[] sorted = widths.toIntArray();
    Arrays.sort(sorted);
    return sorted[Math.min(sorted.length - 1, (int) (quantile * sorted.length))];
  }
}
