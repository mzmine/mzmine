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
 * A chromatogram detected by {@link FastChromatogramBuilder}: the detected data points of one m/z
 * channel in ascending scan order. Contains no zero intensities, flanking zeros are added when the
 * feature is created.
 */
public final class BuiltChromatogram {

  private final double centerMz;
  private final int[] scanIndices;
  private final double[] mzs;
  private final double[] intensities;

  /**
   * @param centerMz    intensity weighted center m/z of the channel
   * @param scanIndices index of each data point in the processed scans, strictly ascending
   * @param mzs         m/z of each data point
   * @param intensities intensity of each data point
   */
  BuiltChromatogram(double centerMz, @NotNull int[] scanIndices, @NotNull double[] mzs,
      @NotNull double[] intensities) {
    if (scanIndices.length != mzs.length || scanIndices.length != intensities.length) {
      throw new IllegalArgumentException("Data arrays differ in length");
    }
    this.centerMz = centerMz;
    this.scanIndices = scanIndices;
    this.mzs = mzs;
    this.intensities = intensities;
  }

  /**
   * @return intensity weighted center m/z of the channel
   */
  public double getCenterMz() {
    return centerMz;
  }

  public int getNumberOfDataPoints() {
    return scanIndices.length;
  }

  /**
   * @return the scan index of data point i in the processed scans
   */
  public int getScanIndex(int i) {
    return scanIndices[i];
  }

  public double getMz(int i) {
    return mzs[i];
  }

  public double getIntensity(int i) {
    return intensities[i];
  }

  /**
   * @return the unweighted mean m/z of all data points, used for the m/z of flanking zeros like in
   * the ADAP chromatogram builder
   */
  public double getMeanMz() {
    double sum = 0d;
    for (final double mz : mzs) {
      sum += mz;
    }
    return mzs.length == 0 ? centerMz : sum / mzs.length;
  }

  public double getMaxIntensity() {
    double max = 0d;
    for (final double intensity : intensities) {
      max = Math.max(max, intensity);
    }
    return max;
  }
}
