/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.SimpleRange;
import org.jetbrains.annotations.NotNull;

/**
 * @param peakIndex         Index of the maximum in the original signal
 * @param peakX             X value of the maximum
 * @param peakY             Y value of the maximum
 * @param snr               Calculated SNR  - mutable
 * @param contributingScale A representative scale
 * @param leftBoundaryIndex Boundary indices - mutable, initialized to invalid
 */
record DetectedPeak(int peakIndex, double peakX, double peakY, double contributingScale, double snr,
                    int leftBoundaryIndex, int rightBoundaryIndex) {

  DetectedPeak(int peakIndex, double peakX, double peakY, double contributingScale) {
    this(peakIndex, peakX, peakY, contributingScale, Double.NaN);
  }

  DetectedPeak(int peakIndex, double peakX, double peakY, double contributingScale, double snr) {
    this(peakIndex, peakX, peakY, snr, contributingScale, -1, -1);
  }

  DetectedPeak(int peakIndex, double peakX, double peakY, double contributingScale, double snr,
      int leftBoundaryIndex, int rightBoundaryIndex) {
    this.peakIndex = peakIndex;
    this.peakX = peakX;
    this.peakY = peakY;
    this.snr = snr; // Allow updating SNR later
    this.contributingScale = contributingScale;
    this.leftBoundaryIndex = leftBoundaryIndex;
    this.rightBoundaryIndex = rightBoundaryIndex;

    if (leftBoundaryIndex < -1 || rightBoundaryIndex < -1
        || leftBoundaryIndex > rightBoundaryIndex) {
      throw new IllegalArgumentException(
          "Invalid boundary indices: " + leftBoundaryIndex + ", " + rightBoundaryIndex);
    }
  }

  DetectedPeak withBoundaries(int left, int right) {
    if (left < 0 || right < 0 || left >= right) {
      throw new IllegalArgumentException("Invalid boundary indices: " + left + ", " + right);
    }
    return new DetectedPeak(peakIndex, peakX, peakY, snr, contributingScale, left, right);
  }

  DetectedPeak withSNR(double snr) {
    return new DetectedPeak(peakIndex, peakX, peakY, contributingScale, snr, leftBoundaryIndex,
        rightBoundaryIndex);
  }

  @NotNull SimpleRange.SimpleIntegerRange indexRange() {
    if(rightBoundaryIndex < 0 || leftBoundaryIndex < 0) {
      throw new IllegalArgumentException("Boundaries not set yet. " + this);
    }
    return SimpleRange.ofInteger(leftBoundaryIndex, rightBoundaryIndex);
  }

  // Check if boundaries are validly set
  boolean hasValidBoundaries(int maxSize) {
    return leftBoundaryIndex >= 0 && rightBoundaryIndex >= 0
        && leftBoundaryIndex <= rightBoundaryIndex && rightBoundaryIndex < maxSize;
  }

  @NotNull
  public Range<Double> asRtRange(double[] x) {
    return Range.closed(x[leftBoundaryIndex], x[rightBoundaryIndex]);
  }

  @Override
  public String toString() {
    return "DetectedPeak{idx=" + peakIndex + ", x=" + String.format("%.2f", peakX) + ", y="
        + String.format("%.2f", peakY) + ", snr=" + String.format("%.2f", snr) + ", scale="
        + String.format("%.2f", contributingScale) + ", bounds=[" + leftBoundaryIndex + ","
        + rightBoundaryIndex + "]" // Added boundaries
        + "}";
  }
}
