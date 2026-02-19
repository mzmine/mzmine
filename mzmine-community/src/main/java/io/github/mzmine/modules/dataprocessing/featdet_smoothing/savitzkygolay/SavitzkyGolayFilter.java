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

package io.github.mzmine.modules.dataprocessing.featdet_smoothing.savitzkygolay;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilities for the Savitzky-Golay smoother.
 */
public class SavitzkyGolayFilter {

  // Cache to store weights for widths that have already been calculated.
  private static final Map<Integer, double[]> WEIGHTS_CACHE = new ConcurrentHashMap<>();

  /**
   * Utility class - no public constructor.
   */
  private SavitzkyGolayFilter() {
    // no public access.
  }

  /**
   * Gets the normalized Savitzky-Golay filter weights for a Quadratic/Cubic polynomial.
   * <p>
   * If the weights for this width exist in the cache, they are returned immediately. Otherwise,
   * they are calculated, cached, and returned.
   * </p>
   *
   * @param width the full width of the filter (must be odd and >= 3).
   * @return the filter weights (normalized).
   */
  public static double[] getNormalizedWeights(int width) {

    if (width == 0) {
      return new double[]{1d};
    }

    if (width % 2 == 0) {
      width += 1;
    }

    // 2. Retrieve from cache or compute if absent
    // This is atomic and thread-safe.
    return WEIGHTS_CACHE.computeIfAbsent(width, SavitzkyGolayFilter::calculateWeights);
  }

  /**
   * Internal method to calculate weights using the analytical formula. Formula: w_k = 3m(m+1) - 1 -
   * 5k^2
   */
  private static double[] calculateWeights(int width) {
    final int m = (width - 1) / 2; // The half-width
    final double[] weights = new double[width];
    double sum = 0.0;

    // Calculate raw weights for Polynomial Order 2 (Quadratic) / 3 (Cubic)
    for (int i = 0; i < width; i++) {
      int k = i - m; // Distance from center

      // Analytical formula
      double val = 3.0 * m * (m + 1) - 1 - 5.0 * k * k;

      weights[i] = val;
      sum += val;
    }

    // Normalize
    for (int i = 0; i < width; i++) {
      weights[i] /= sum;
    }

    return weights;
  }

  /**
   * Convolve a set of weights with a set of intensities.
   *
   * @param intensities the intensities.
   * @param weights     the filter weights.
   * @return the convolution results.
   */
  public static double[] convolve(final double[] intensities, final double[] weights) {

    // Initialise.
    final int fullWidth = weights.length;
    final int halfWidth = (fullWidth - 1) / 2;
    final int numPoints = intensities.length;

    // Convolve.
    final double[] convolved = new double[numPoints];
    for (int i = 0; i < numPoints; i++) {

      double sum = 0.0;
      final int k = i - halfWidth;

      // Boundary handling: strict intersection of filter and data
      int startJ = Math.max(0, -k);
      int endJ = Math.min(fullWidth, numPoints - k);

      for (int j = startJ; j < endJ; j++) {
        sum += intensities[k + j] * weights[j];
      }

      // Set the result.
      convolved[i] = sum;
    }

    return convolved;
  }

  /**
   * Returns the valid filter width. Ensures the width is odd and at least 3.
   */
  public static int getClosestFilterWidth(int width) {
    if (width < 3) {
      return 3;
    }
    // Ensure odd
    return (width % 2 == 0) ? width + 1 : width;
  }
}