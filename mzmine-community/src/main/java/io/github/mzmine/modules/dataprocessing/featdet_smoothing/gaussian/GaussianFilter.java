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

package io.github.mzmine.modules.dataprocessing.featdet_smoothing.gaussian;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilities for a discrete Gaussian smoother. In contrast to the Savitzky-Golay filter, the
 * Gaussian kernel has strictly non-negative weights, so it does not produce overshoot/undershoot
 * (ringing) next to sharp, intense peaks. This avoids introducing artificial local maxima on the
 * flanks of such peaks - a property of Gaussian scale-space smoothing (it never creates new local
 * extrema).
 */
public class GaussianFilter {

  // Cache to store weights for widths that have already been calculated.
  private static final Map<Integer, double[]> WEIGHTS_CACHE = new ConcurrentHashMap<>();

  private GaussianFilter() {
    // no public access.
  }

  /**
   * Gets the normalized Gaussian filter weights. The standard deviation is derived from the width
   * as {@code sigma = width / 6}, so the full width spans roughly +/- 3 sigma.
   *
   * @param width the full width of the filter. Even numbers are converted to next higher odd
   *              number.
   * @return the normalized filter weights (sum to 1).
   */
  public static double[] getNormalizedWeights(int width) {
    if (width <= 1) {
      return new double[]{1d};
    }
    if (width % 2 == 0) {
      width += 1;
    }
    return WEIGHTS_CACHE.computeIfAbsent(width, GaussianFilter::calculateWeights);
  }

  private static double[] calculateWeights(final int width) {
    final int m = (width - 1) / 2; // half-width
    final double sigma = width / 6.0; // full width ~ 6 sigma (+/- 3 sigma)
    final double twoSigmaSq = 2.0 * sigma * sigma;

    final double[] weights = new double[width];
    double sum = 0.0;
    for (int i = 0; i < width; i++) {
      final int k = i - m; // distance from center
      final double value = Math.exp(-(k * k) / twoSigmaSq);
      weights[i] = value;
      sum += value;
    }

    // normalize so the weights sum to 1
    for (int i = 0; i < width; i++) {
      weights[i] /= sum;
    }
    return weights;
  }

  /**
   * Returns the valid filter width. Ensures the width is odd and at least 3.
   */
  public static int getClosestFilterWidth(int width) {
    if (width < 3) {
      return 3;
    }
    return (width % 2 == 0) ? width + 1 : width;
  }
}
