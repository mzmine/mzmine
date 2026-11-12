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

import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Detected data points of one chromatogram without zeros, independent of the builder that created
 * it.
 *
 * @param scans       scan index of each data point, ascending
 * @param mzs         m/z of each data point
 * @param intensities intensity of each data point
 */
record EvaluatedChromatogram(@NotNull int[] scans, @NotNull double[] mzs,
                             @NotNull double[] intensities) {

  @NotNull
  static EvaluatedChromatogram of(@NotNull BuiltChromatogram c) {
    final int n = c.getNumberOfDataPoints();
    final int[] scans = new int[n];
    final double[] mzs = new double[n];
    final double[] intensities = new double[n];
    for (int i = 0; i < n; i++) {
      scans[i] = c.getScanIndex(i);
      mzs[i] = c.getMz(i);
      intensities[i] = c.getIntensity(i);
    }
    return new EvaluatedChromatogram(scans, mzs, intensities);
  }

  @NotNull
  static List<EvaluatedChromatogram> of(@NotNull List<BuiltChromatogram> chromatograms) {
    return chromatograms.stream().map(EvaluatedChromatogram::of).toList();
  }

  int size() {
    return scans.length;
  }

  /**
   * @return intensity weighted mean m/z or 0 if empty
   */
  double weightedMz() {
    double sum = 0;
    double weights = 0;
    for (int i = 0; i < mzs.length; i++) {
      sum += mzs[i] * intensities[i];
      weights += intensities[i];
    }
    return weights > 0 ? sum / weights : 0;
  }

  /**
   * @return index of the most intense data point or -1 if empty
   */
  int apexIndex() {
    if (intensities.length == 0) {
      return -1;
    }
    int apex = 0;
    for (int i = 1; i < intensities.length; i++) {
      if (intensities[i] > intensities[apex]) {
        apex = i;
      }
    }
    return apex;
  }
}
