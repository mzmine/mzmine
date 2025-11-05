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
import org.jetbrains.annotations.Nullable;
class DetectedPeak {

  final int peakIndex;     // Index of the maximum in the original signal
  final double peakX;      // X value of the maximum
  final double peakY;      // Y value of the maximum
  double snr;         // Calculated SNR  - mutable
  final double contributingScale; // A representative scale

  // Boundary indices - mutable, initialized to invalid
  int leftBoundaryIndex = -1;
  int rightBoundaryIndex = -1;

  public DetectedPeak(int peakIndex, double peakX, double peakY, double snr, double contributingScale) {
    this.peakIndex = peakIndex;
    this.peakX = peakX;
    this.peakY = peakY;
    this.snr = snr; // Allow updating SNR later
    this.contributingScale = contributingScale;
  }

  // Setter for boundary indices
  public void setBoundaryIndices(int left, int right) {
    // Add basic validation if desired
    if (left <= right && left >= 0) { // Basic check
      this.leftBoundaryIndex = left;
      this.rightBoundaryIndex = right;
    } else {
      this.leftBoundaryIndex = -1; // Mark as invalid
      this.rightBoundaryIndex = -1;
    }
  }

  // Getter for boundary range (optional, for convenience)
  @Nullable
  public Range<Integer> getBoundaryIndexRange(int maxSize) {
    if (hasValidBoundaries(maxSize)) {
      return Range.closed(leftBoundaryIndex, rightBoundaryIndex);
    }
    return null; // Indicate invalid/not set
  }

  // Check if boundaries are validly set
  public boolean hasValidBoundaries(int maxSize) {
    return leftBoundaryIndex >= 0 && rightBoundaryIndex >= 0
        && leftBoundaryIndex <= rightBoundaryIndex && rightBoundaryIndex < maxSize;
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
