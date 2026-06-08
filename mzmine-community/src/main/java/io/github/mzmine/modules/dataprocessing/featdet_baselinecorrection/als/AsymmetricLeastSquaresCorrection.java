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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.als;

/**
 * Performs asymmetric least squares baseline correction on an array of data
 */
public class AsymmetricLeastSquaresCorrection {

  /**
   * Performs asymmetric least squares baseline correction on an array of data.
   *
   * @param y     The input data array.
   * @param lambda   The smoothness parameter (lambda); common value is around 1e6.
   * @param p     The asymmetry parameter (0 < p < 1); common value is around 0.001.
   * @param nIter The number of iterations; common is 10 to 20.
   * @return The baseline
   */
  public static double[] asymmetricLeastSquaresBaseline(double[] y, double lambda, double p,
      int nIter) {
    int n = y.length;
    double[] w = new double[n];
    double[] z = new double[n];

    for (int i = 0; i < n; i++) {
      w[i] = 1.0;
    }

    for (int i = 0; i < nIter; i++) {
      double[] W = diag(w);
      z = solve(W, lambda, y);

      for (int j = 0; j < n; j++) {
        w[j] = y[j] > z[j] ? p : (1.0 - p);
      }
    }

    return z;
  }

  private static double[] diag(double[] w) {
    // Returns the diagonal matrix as a flat array.
    return w.clone();
  }

  private static double[] solve(double[] W, double lam, double[] y) {
    int n = y.length;
    double[] d = new double[n];
    double[] l = new double[n - 1];
    double[] u = new double[n - 1];

    for (int i = 0; i < n; i++) {
      d[i] = W[i] + (i == 0 || i == n - 1 ? lam : 2 * lam);
    }
    for (int i = 0; i < n - 1; i++) {
      u[i] = l[i] = -lam;
    }

    return thomasAlgorithm(l, d, u, y);
  }

  /**
   * Solves a tridiagonal system Ax = b using the Thomas algorithm.
   *
   * @param l The subdiagonal elements, where l[i] is the element directly below the i-th diagonal
   *          element.
   * @param d The main diagonal elements, where d[i] is the i-th diagonal element.
   * @param u The superdiagonal elements, where u[i] is the element directly above the i-th diagonal
   *          element.
   * @param b The right-hand side vector of the equation Ax = b.
   * @return The solution vector x.
   */
  private static double[] thomasAlgorithm(double[] l, double[] d, double[] u, double[] b) {
    int n = d.length;
    double[] cPrime = new double[n - 1];
    double[] dPrime = new double[n];

    // Forward sweep
    cPrime[0] = u[0] / d[0];
    dPrime[0] = b[0] / d[0];

    for (int i = 1; i < n - 1; i++) {
      double m = 1.0 / (d[i] - l[i - 1] * cPrime[i - 1]);
      cPrime[i] = u[i] * m;
      dPrime[i] = (b[i] - l[i - 1] * dPrime[i - 1]) * m;
    }

    dPrime[n - 1] = (b[n - 1] - l[n - 2] * dPrime[n - 2]) / (d[n - 1] - l[n - 2] * cPrime[n - 2]);

    // Backward substitution
    double[] x = new double[n];
    x[n - 1] = dPrime[n - 1];
    for (int i = n - 2; i >= 0; i--) {
      x[i] = dPrime[i] - cPrime[i] * x[i + 1];
    }

    return x;
  }

  /**
   * subtracts elements in z from the corresponding element in y and returns a new array.
   */
  public static double[] subtract(double[] y, double[] z) {
    double[] result = new double[y.length];
    for (int i = 0; i < y.length; i++) {
      result[i] = y[i] - z[i];
    }
    return result;
  }

}
