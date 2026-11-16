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

/**
 * Rich descriptor of a single peak detected by the {@link LocalMaxMassDetector}. Indices refer to
 * the spectrum's data point indices (aligned with the spectrum's m/z values). Used for
 * analysis/diagnostics via {@link LocalMaxMassDetector#detectPeaks}.
 *
 * @param apexIndex           index of the apex (maximum of the original intensities within the
 *                            detected edges).
 * @param leftIndex           inclusive start index of the peak region (left edge / valley).
 * @param rightIndexExclusive exclusive end index of the peak region (right edge / valley).
 * @param centroidMz          the centroided m/z (as reported by the detector).
 * @param height              the apex intensity in the original (unsmoothed) data.
 */
public record LocalMaxPeak(int apexIndex, int leftIndex, int rightIndexExclusive, double centroidMz,
                           double height) {

  /**
   * Number of points on the left flank (from the left edge up to, but excluding, the apex).
   */
  public int leftPoints() {
    return apexIndex - leftIndex;
  }

  /**
   * Number of points on the right flank (from the apex, exclusive, to the right edge).
   */
  public int rightPoints() {
    return rightIndexExclusive - 1 - apexIndex;
  }
}
