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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.savitzkygolay;

public final class SGDerivative {

  /**
   * This method returns the second smoothed derivative values of an array.
   * 
   * @param values
   * @param firstDerivative is first derivative
   * @param levelOfFilter level of filter (1 - 12)
   * @return double[] derivative of values
   */
  public static double[] calculateDerivative(double[] values, boolean firstDerivative,
      int levelOfFilter) {

    double[] derivative = new double[values.length];
    int M = 0;

    for (int k = 0; k < derivative.length; k++) {

      // Determine boundaries
      if (k <= levelOfFilter)
        M = k;
      if (k + M > derivative.length - 1)
        M = derivative.length - (k + 1);

      // Perform derivative using Savitzky Golay coefficients
      for (int i = -M; i <= M; i++) {
        derivative[k] += values[k + i] * getSGCoefficient(M, i, firstDerivative);
      }
      // if ((Math.abs(derivative[k])) > maxValueDerivative)
      // maxValueDerivative = Math.abs(derivative[k]);

    }

    return derivative;
  }

  /**
   * This method return the Savitzky-Golay 2nd smoothed derivative coefficient from an array
   * 
   * @param M
   * @param signedC
   * @return
   */
  private static Double getSGCoefficient(int M, int signedC, boolean firstDerivate) {

    int C = Math.abs(signedC), sign = 1;
    if (firstDerivate) {
      if (signedC < 0)
        sign = -1;
      return sign * SGCoefficients.SGCoefficientsFirstDerivativeQuartic[M][C];
    } else {
      return SGCoefficients.SGCoefficientsSecondDerivative[M][C];
    }

  }

}
