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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.arpls;

import org.jetbrains.annotations.NotNull;

/**
 * Direct solver for a symmetric positive-definite pentadiagonal linear system {@code A x = rhs}.
 * The matrix is given by its three upper diagonals and factored in place as {@code A = L D L^T}
 * with a unit lower-triangular {@code L} that carries two sub-diagonals. Both the factorization and
 * the solve run in O(n), which keeps the arPLS baseline fit linear in the number of data points
 * instead of the O(n^3) of a dense solve.
 * <p>
 * The instance is a reusable workspace whose buffers grow on demand (see {@link #ensureCapacity}).
 * The actual system size {@code n} is passed to {@link #factor} / {@link #solve}, so the same
 * workspace can be reused for traces of varying length without re-allocating - the buffers may be
 * longer than {@code n} and only their first {@code n} entries are used.
 */
class PentadiagonalSolver {

  // pivots D
  private double[] d = new double[0];
  // L sub-diagonal L[i][i-1]
  private double[] alpha = new double[0];
  // L sub-sub-diagonal L[i][i-2]
  private double[] beta = new double[0];

  /**
   * Grows the factorization buffers so they can hold a system of size {@code n}. Existing contents
   * are discarded; they are fully overwritten by the next {@link #factor}.
   *
   * @param n the required capacity
   */
  public void ensureCapacity(final int n) {
    if (d.length < n) {
      d = new double[n];
      alpha = new double[n];
      beta = new double[n];
    }
  }

  /**
   * Factors the matrix described by its upper diagonals into the reused buffers. The input arrays
   * are only read, not modified.
   *
   * @param a main diagonal {@code A[i][i]}, first n entries used
   * @param b first upper diagonal {@code A[i][i+1]}, first n - 1 entries used
   * @param c second upper diagonal {@code A[i][i+2]}, first n - 2 entries used
   * @param n the system size
   */
  public void factor(final double @NotNull [] a, final double @NotNull [] b,
      final double @NotNull [] c, final int n) {
    ensureCapacity(n);
    for (int i = 0; i < n; i++) {
      final double dm1 = i >= 1 ? d[i - 1] : 0d;
      final double dm2 = i >= 2 ? d[i - 2] : 0d;

      beta[i] = i >= 2 ? c[i - 2] / dm2 : 0d;
      if (i >= 1) {
        final double correction = i >= 2 ? beta[i] * alpha[i - 1] * dm2 : 0d;
        alpha[i] = (b[i - 1] - correction) / dm1;
      } else {
        alpha[i] = 0d;
      }

      double di = a[i];
      if (i >= 1) {
        di -= alpha[i] * alpha[i] * dm1;
      }
      if (i >= 2) {
        di -= beta[i] * beta[i] * dm2;
      }
      d[i] = di;
    }
  }

  /**
   * Solves {@code A x = rhs} for the most recently {@link #factor factored} matrix and writes the
   * solution into {@code out}. {@code out} must differ from {@code rhs} (it is written while
   * {@code rhs} is still read).
   *
   * @param rhs right-hand side, first n entries used
   * @param out target for the solution, first n entries written
   * @param n   the system size
   */
  public void solve(final double @NotNull [] rhs, final double @NotNull [] out, final int n) {
    // forward substitution: L y = rhs (y stored in out)
    for (int i = 0; i < n; i++) {
      double v = rhs[i];
      if (i >= 1) {
        v -= alpha[i] * out[i - 1];
      }
      if (i >= 2) {
        v -= beta[i] * out[i - 2];
      }
      out[i] = v;
    }

    // diagonal solve: D w = y
    for (int i = 0; i < n; i++) {
      out[i] /= d[i];
    }

    // backward substitution: L^T x = w
    for (int i = n - 1; i >= 0; i--) {
      double v = out[i];
      if (i + 1 < n) {
        v -= alpha[i + 1] * out[i + 1];
      }
      if (i + 2 < n) {
        v -= beta[i + 2] * out[i + 2];
      }
      out[i] = v;
    }
  }
}
