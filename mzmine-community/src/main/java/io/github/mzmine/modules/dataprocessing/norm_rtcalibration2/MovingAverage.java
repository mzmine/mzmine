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

package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2;

/**
 * MovingAverage - A class that calculates the moving average of an array of values. The resulting
 * array will have the same length as the input array. The first and last values of the input array
 * are preserved in the output.
 */
public class MovingAverage {

  /**
   * Calculates the moving average of an array of double values.
   *
   * @param values     The input array of values
   * @param windowSize The size of the moving window
   * @return An array of the same size with moving average values
   * @throws IllegalArgumentException If windowSize is not positive or exceeds array length
   */
  public static double[] calculate(double[] values, int windowSize) {
    if (values == null || values.length == 0) {
      return new double[0];
    }

    if (windowSize <= 0) {
      throw new IllegalArgumentException("Window size must be positive");
    }

    if (windowSize > values.length) {
      throw new IllegalArgumentException("Window size cannot exceed array length");
    }

    int n = values.length;
    double[] result = new double[n];

    // Initialize first value (unchanged)
    result[0] = values[0];

    // Calculate middle values with moving average
    int actualSize = 0;
    double sum = 0;

    for (int i = 1; i < n - 1; i++) {
      actualSize++;
      sum += values[i];
      if(actualSize >= windowSize) {
        sum -= values[i-actualSize];
        actualSize--;
      }

      result[i] = sum / actualSize;
    }

    // Initialize last value (unchanged)
    if (n > 1) {
      result[n - 1] = values[n - 1];
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
