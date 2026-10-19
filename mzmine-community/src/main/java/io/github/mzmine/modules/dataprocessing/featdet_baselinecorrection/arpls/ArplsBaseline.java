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
import org.jetbrains.annotations.NotNull;

/**
 * Asymmetrically reweighted penalized least squares (arPLS) baseline estimation after Baek et al.,
 * Analyst 2015, 140, 250-257. The baseline {@code z} minimizes
 * {@code |W^(1/2) (y - z)|^2 + lambda |D2 z|^2}, where {@code D2} is the second-order difference
 * operator and {@code W} is a diagonal weight matrix. The weights are iteratively updated with a
 * logistic function of the residual statistics so that points above the baseline (peaks) lose their
 * influence while points below it keep it, which pulls the estimate onto the lower envelope of the
 * signal without any explicit peak detection.
 * <p>
 * assumption: the samples are (approximately) equally spaced, so the {@code D2} penalty uses the
 * index distance rather than the retention time - which holds for chromatographic detector traces.
 * <p>
 * The instance is a reusable workspace: its buffers grow on demand (see {@link #ensureCapacity})
 * and are reused across {@link #fitBaseline} calls. The buffers may be longer than the current
 * number of values {@code n}; only their first {@code n} entries are ever used, so the same
 * instance can correct traces of varying length without re-allocating.
 */
class ArplsBaseline {

  // constant penalty band lambda * D2^T D2 (before scaling by lambda)
  private double[] penDiag = new double[0];
  private double[] penOff1 = new double[0];
  private double[] penOff2 = new double[0];
  // weights and the linear system A = W + lambda * D2^T D2 with right-hand side W y
  private double[] w = new double[0];
  private double[] diag = new double[0];
  private double[] off1 = new double[0];
  private double[] off2 = new double[0];
  private double[] rhs = new double[0];
  // the estimated baseline (also the solver output buffer)
  private double[] baseline = new double[0];
  private final PentadiagonalSolver solver = new PentadiagonalSolver();

  /**
   * Grows all workspace buffers so they can hold a trace of {@code n} values.
   *
   * @param n the required capacity
   */
  public void ensureCapacity(final int n) {
    if (baseline.length < n) {
      penDiag = new double[n];
      penOff1 = new double[n];
      penOff2 = new double[n];
      w = new double[n];
      diag = new double[n];
      off1 = new double[n];
      off2 = new double[n];
      rhs = new double[n];
      baseline = new double[n];
    }
    solver.ensureCapacity(n);
  }

  /**
   * Estimates the baseline of the given signal into the reused {@link #baseline} buffer.
   *
   * @param y             the intensities; only the first {@code n} values are read
   * @param n             the number of values to use
   * @param lambda        smoothness penalty. Larger values yield a stiffer, smoother baseline
   * @param ratio         convergence threshold on the relative change of the weight vector
   * @param maxIterations maximum number of reweighting iterations
   * @return the reused baseline buffer. Only its first {@code n} entries are valid; the array may
   * be longer than {@code n} and is overwritten by the next call.
   */
  public double @NotNull [] fitBaseline(final double @NotNull [] y, final int n,
      final double lambda, final double ratio, final int maxIterations) {
    ensureCapacity(n);
    if (n < 3) {
      // not enough points for a second-order difference penalty
      Arrays.fill(baseline, 0, n, 0d);
      return baseline;
    }

    // rebuild the penalty band by accumulating the outer product of the [1, -2, 1] stencil, so the
    // array boundaries are handled automatically. The accumulation targets are zeroed first because
    // the buffers are reused and may still hold values from a previous (longer) trace.
    Arrays.fill(penDiag, 0, n, 0d);
    Arrays.fill(penOff1, 0, n - 1, 0d);
    Arrays.fill(penOff2, 0, n - 2, 0d);
    for (int k = 0; k <= n - 3; k++) {
      penDiag[k] += 1d;
      penDiag[k + 1] += 4d;
      penDiag[k + 2] += 1d;
      penOff1[k] += -2d;
      penOff1[k + 1] += -2d;
      penOff2[k] += 1d;
    }

    Arrays.fill(w, 0, n, 1d);

    // off diagonals only depend on the constant penalty band
    for (int i = 0; i < n - 1; i++) {
      off1[i] = lambda * penOff1[i];
    }
    for (int i = 0; i < n - 2; i++) {
      off2[i] = lambda * penOff2[i];
    }

    for (int iter = 0; iter < maxIterations; iter++) {
      // only the diagonal and right-hand side change between iterations (through the weights)
      for (int i = 0; i < n; i++) {
        diag[i] = w[i] + lambda * penDiag[i];
        rhs[i] = w[i] * y[i];
      }

      solver.factor(diag, off1, off2, n);
      solver.solve(rhs, baseline, n);

      // statistics of the residuals that lie below the current baseline (d < 0)
      double sum = 0d;
      int count = 0;
      for (int i = 0; i < n; i++) {
        final double d = y[i] - baseline[i];
        if (d < 0d) {
          sum += d;
          count++;
        }
      }
      if (count == 0) {
        // baseline already sits at or below every point
        return baseline;
      }
      final double mean = sum / count;
      double sq = 0d;
      for (int i = 0; i < n; i++) {
        final double d = y[i] - baseline[i];
        if (d < 0d) {
          final double e = d - mean;
          sq += e * e;
        }
      }
      final double std = Math.sqrt(sq / count);
      if (std == 0d) {
        return baseline;
      }

      // logistic reweighting: ~1 well below the baseline, ~0 for strong positive (peak) residuals
      final double cutoff = 2d * std - mean;
      double changeNum = 0d;
      double changeDen = 0d;
      for (int i = 0; i < n; i++) {
        final double d = y[i] - baseline[i];
        final double wt = 1d / (1d + Math.exp(2d * (d - cutoff) / std));
        final double diff = wt - w[i];
        changeNum += diff * diff;
        changeDen += w[i] * w[i];
        w[i] = wt;
      }

      // relative change of the weight vector, as in the original arPLS termination criterion
      final double change = changeDen > 0d ? Math.sqrt(changeNum) / Math.sqrt(changeDen) : 0d;
      if (change < ratio) {
        break;
      }
    }
    return baseline;
  }
}
