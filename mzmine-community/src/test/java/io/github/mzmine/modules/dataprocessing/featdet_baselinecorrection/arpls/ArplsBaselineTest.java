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

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ArplsBaselineTest {

  /**
   * Builds a strictly diagonally dominant (hence symmetric positive-definite) pentadiagonal matrix,
   * multiplies it with a known x to get the right-hand side, and checks that the solver recovers
   * x.
   */
  @Test
  void pentadiagonalSolverRecoversKnownSolution() {
    final int n = 50;
    final Random rnd = new Random(42);

    final double[] diag = new double[n];
    final double[] off1 = new double[n - 1];
    final double[] off2 = new double[n - 2];
    for (int i = 0; i < n - 1; i++) {
      off1[i] = rnd.nextDouble() - 0.5d;
    }
    for (int i = 0; i < n - 2; i++) {
      off2[i] = rnd.nextDouble() - 0.5d;
    }
    // make each row diagonally dominant -> SPD
    for (int i = 0; i < n; i++) {
      double rowSum = 0d;
      if (i >= 1) {
        rowSum += Math.abs(off1[i - 1]);
      }
      if (i < n - 1) {
        rowSum += Math.abs(off1[i]);
      }
      if (i >= 2) {
        rowSum += Math.abs(off2[i - 2]);
      }
      if (i < n - 2) {
        rowSum += Math.abs(off2[i]);
      }
      diag[i] = rowSum + 1d;
    }

    final double[] x = new double[n];
    for (int i = 0; i < n; i++) {
      x[i] = rnd.nextDouble() * 10d;
    }
    final double[] rhs = multiply(diag, off1, off2, x);

    final PentadiagonalSolver solver = new PentadiagonalSolver();
    solver.factor(diag, off1, off2, n);
    final double[] solved = new double[n];
    solver.solve(rhs, solved, n);

    Assertions.assertArrayEquals(x, solved, 1e-9);
  }

  /**
   * A linear baseline with a single Gaussian peak: arPLS should recover the linear baseline in the
   * peak-free regions and must not run through the peak.
   */
  @Test
  void fitBaselineRecoversLinearBaselineUnderPeak() {
    final int n = 200;
    final double intercept = 10d;
    final double slope = 0.05d;
    final double[] trueBaseline = new double[n];
    final double[] signal = new double[n];
    for (int i = 0; i < n; i++) {
      trueBaseline[i] = intercept + slope * i;
      final double peak = 50d * Math.exp(-Math.pow((i - 100) / 5d, 2) / 2d);
      signal[i] = trueBaseline[i] + peak;
    }

    final double[] baseline = new ArplsBaseline().fitBaseline(signal, n, 1e4, 1e-8, 100);

    Assertions.assertTrue(baseline.length >= n);
    // peak-free regions (well away from index 100) must match the true linear baseline closely
    for (int i = 0; i < n; i++) {
      if (Math.abs(i - 100) > 30) {
        Assertions.assertEquals(trueBaseline[i], baseline[i], 0.5d,
            "baseline off in peak-free region at " + i);
      }
      // the baseline must never rise above the signal by more than a tiny epsilon
      Assertions.assertTrue(baseline[i] <= signal[i] + 1e-6, "baseline above signal at " + i);
    }
    // and it must stay well below the peak apex
    Assertions.assertTrue(baseline[100] < signal[100] - 30d, "baseline climbed into the peak");
  }

  @Test
  void fitBaselineHandlesTooFewPoints() {
    final double[] baseline = new ArplsBaseline().fitBaseline(new double[]{1d, 2d}, 2, 1e5, 1e-6,
        50);
    Assertions.assertArrayEquals(new double[]{0d, 0d}, baseline);
  }

  /**
   * A reused instance must give exactly the same result as a fresh one, even after first processing
   * a longer trace: only the first {@code n} entries may be touched and no stale state may leak.
   */
  @Test
  void reusedInstanceMatchesFreshInstanceOnShorterTrace() {
    final ArplsBaseline reused = new ArplsBaseline();

    // first process a long trace so the buffers grow and hold values from it
    final double[] longSignal = new double[300];
    for (int i = 0; i < longSignal.length; i++) {
      longSignal[i] = 5d + 0.02d * i + 80d * Math.exp(-Math.pow((i - 150) / 4d, 2) / 2d);
    }
    reused.fitBaseline(longSignal, longSignal.length, 1e4, 1e-8, 100);

    // then a shorter, differently shaped trace on the same (reused) instance
    final int shortN = 80;
    final double[] shortSignal = new double[shortN];
    for (int i = 0; i < shortN; i++) {
      shortSignal[i] = 20d - 0.1d * i + 40d * Math.exp(-Math.pow((i - 40) / 3d, 2) / 2d);
    }
    final double[] reusedResult = Arrays.copyOf(
        reused.fitBaseline(shortSignal, shortN, 1e4, 1e-8, 100), shortN);

    final double[] freshResult = Arrays.copyOf(
        new ArplsBaseline().fitBaseline(shortSignal, shortN, 1e4, 1e-8, 100), shortN);

    Assertions.assertArrayEquals(freshResult, reusedResult, 1e-12);
  }

  private static double[] multiply(final double[] diag, final double[] off1, final double[] off2,
      final double[] x) {
    final int n = diag.length;
    final double[] r = new double[n];
    for (int i = 0; i < n; i++) {
      double v = diag[i] * x[i];
      if (i >= 1) {
        v += off1[i - 1] * x[i - 1];
      }
      if (i < n - 1) {
        v += off1[i] * x[i + 1];
      }
      if (i >= 2) {
        v += off2[i - 2] * x[i - 2];
      }
      if (i < n - 2) {
        v += off2[i] * x[i + 2];
      }
      r[i] = v;
    }
    return r;
  }
}
