/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import java.util.HashMap;
import java.util.Map;

/**
 * Utilities for the Savitzky-Golay smoother.
 *
 * @author $Author$
 * @version $Revision$
 */
public class SavitzkyGolayFilter {

  // The filter values.
  private static final Map<Integer, int[]> VALUES = new HashMap<Integer, int[]>(11);

  static {

    // Load the values.
    VALUES.put(5, new int[] {17, 12, -3});
    VALUES.put(7, new int[] {7, 6, 3, -2});
    VALUES.put(9, new int[] {59, 54, 39, 14, -21});
    VALUES.put(11, new int[] {89, 84, 69, 44, 9, -36});
    VALUES.put(13, new int[] {25, 24, 21, 16, 9, 0, -11});
    VALUES.put(15, new int[] {167, 162, 147, 122, 87, 42, -13, -78});
    VALUES.put(17, new int[] {43, 42, 39, 34, 27, 18, 7, -6, -21});
    VALUES.put(19, new int[] {269, 264, 249, 224, 189, 144, 89, 24, -51, -136});
    VALUES.put(21, new int[] {329, 324, 309, 284, 249, 204, 149, 84, 9, -76, -171});
    VALUES.put(23, new int[] {79, 78, 75, 70, 63, 54, 43, 30, 15, -2, -21, -42});
    VALUES.put(25, new int[] {467, 462, 447, 422, 387, 343, 287, 222, 147, 62, -33, -138, -253});
  }

  /**
   * Utility class - no public constructor.
   */
  private SavitzkyGolayFilter() {

    // no public access.
  }

  /**
   * Gets the normalized Savitzky-Golay filter weights.
   *
   * @param width the full width of the filter.
   * @return the filter weights (normalized).
   */
  public static double[] getNormalizedWeights(final int width) {

    if (width == 0) {
      return new double[]{1d};
    }

    // Get the raw values.
    final int[] values = VALUES.get(width);
    if (values == null) {
      throw new IllegalArgumentException(
          "No Savitzky-Golay filter of width " + width + " is defined");
    }

    // Copy values (symmetrically) to weights array.
    final int vLen = values.length;
    final int wLen = vLen * 2 - 1;
    final int vEnd = vLen - 1;
    final double[] weights = new double[wLen];
    int sum = values[0];
    weights[vEnd] = (double) sum;
    for (int i = 1; i < vLen; i++) {

      final int value = values[i];
      weights[vEnd + i] = (double) value;
      weights[vEnd - i] = (double) value;
      sum += 2 * value;
    }

    // Normalize.
    for (int i = 0; i < wLen; i++) {
      weights[i] /= (double) sum;
    }

    return weights;
  }

  /**
   * Convolve a set of weights with a set of intensities.
   *
   * @param intensities the intensities.
   * @param weights the filter weights.
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
      for (int j = Math.max(0, -k); j < Math.min(fullWidth, numPoints - k); j++) {
        sum += intensities[k + j] * weights[j];
      }

      // Set the result.
      convolved[i] = sum;
    }

    return convolved;
  }
}
