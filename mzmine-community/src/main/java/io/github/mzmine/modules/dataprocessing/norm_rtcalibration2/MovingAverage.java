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

package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2;

import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MovingAverage - A class that calculates the moving average of an array of values. The resulting
 * array will have the same length as the input array. The first and last values of the input array
 * are preserved in the output.
 */
public class MovingAverage {

  private static final Logger logger = Logger.getLogger(MovingAverage.class.getName());

  /**
   * Calculates the moving average of an array of double values.
   *
   * @param values     The input array of values
   * @param windowSize The size of the moving window, needs to be odd otherwise the window+1 will be
   *                   used
   * @return An array of the same size with moving average values
   * @throws IllegalArgumentException If windowSize is not positive or exceeds array length
   */
  public static double @NotNull [] calculate(double @Nullable [] values, int windowSize) {
    if (values == null || values.length == 0) {
      return new double[0];
    }
    if (windowSize <= 1) {
      throw new IllegalArgumentException("windowSize must be > 1 and odd");
    }
    if (windowSize % 2 == 0) {
      windowSize++;
      logger.warning("Window size must be odd, so using window size+1 instead: " + windowSize);
    }

    int n = values.length;
    double[] result = new double[n];
    int halfWindow = windowSize / 2;

    for (int i = 0; i < n; i++) {
      // window is smaller on the edges. data points 0, 1, 2, 3 have half windows 0, 1, 2, 3...
      int actualHalfWindow = halfWindow;
      if (i < actualHalfWindow) {
        actualHalfWindow = i;
      }
      if (n - i - 1 < actualHalfWindow) {
        actualHalfWindow = n - i - 1;
      }

      int start = i - actualHalfWindow;
      int end = i + actualHalfWindow;

      double sum = 0.0;

      // for now just recompute the window each time. optimizations possible with sliding window
      // but complex due to handling of edge cases start and end
      for (int j = start; j <= end; j++) {
        sum += values[j];
      }

      result[i] = sum / (end - start + 1);
    }
    return result;
  }

  /**
   * Overloaded method to support float arrays.
   *
   * @param values     The input array of values
   * @param windowSize The size of the moving window
   * @return An array of the same size with moving average values
   * @throws IllegalArgumentException If windowSize is not positive or exceeds array length
   */
  public static float[] calculate(float[] values, int windowSize) {
    if (values == null || values.length == 0) {
      return new float[0];
    }

    double[] doubleValues = new double[values.length];
    for (int i = 0; i < values.length; i++) {
      doubleValues[i] = values[i];
    }

    double[] result = calculate(doubleValues, windowSize);

    float[] floatResult = new float[result.length];
    for (int i = 0; i < result.length; i++) {
      floatResult[i] = (float) result[i];
    }

    return floatResult;
  }

  /**
   * Overloaded method to support int arrays.
   *
   * @param values     The input array of values
   * @param windowSize The size of the moving window
   * @return An array of the same size with moving average values
   * @throws IllegalArgumentException If windowSize is not positive or exceeds array length
   */
  public static double[] calculate(int[] values, int windowSize) {
    if (values == null || values.length == 0) {
      return new double[0];
    }

    double[] doubleValues = new double[values.length];
    for (int i = 0; i < values.length; i++) {
      doubleValues[i] = values[i];
    }

    return calculate(doubleValues, windowSize);
  }
}
