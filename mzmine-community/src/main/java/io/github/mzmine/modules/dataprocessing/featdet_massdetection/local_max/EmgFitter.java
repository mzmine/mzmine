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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.local_max;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Weighted least squares fit of an exponentially modified Gaussian (EMG, Gaussian convolved with an
 * exponential tail to higher m/z) to a profile peak. The model is scaled to its maximum so that the
 * fitted height is the apex height and can be bounded by the raw apex. The apex m/z is the mode of
 * the EMG, not its Gaussian center.
 */
public final class EmgFitter {

  /**
   * Fitted EMG in real units.
   *
   * @param height apex height
   * @param mu     center of the Gaussian component
   * @param sigma  sigma of the Gaussian component
   * @param tau    time constant of the exponential tail (> 0: tail to higher m/z)
   * @param mode   m/z of the apex
   */
  public record Fit(double height, double mu, double sigma, double tau, double mode) {

    public double value(final double mz) {
      return height * EmgMath.shape(mz, mu, sigma, tau) / EmgMath.shape(mode, mu, sigma, tau);
    }
  }

  private EmgFitter() {
  }

  /**
   * @param center      reference of the normalized x axis, start value of the apex
   * @param startSigma  start width in m/z, also the x normalization
   * @param startHeight start value of the apex height
   * @param minHeight   lower bound of the apex height (0 = unbounded)
   * @return the fit or null if the optimizer failed
   */
  public static @Nullable Fit fit(final double @NotNull [] mzs,
      final double @NotNull [] intensities, final double @NotNull [] weights, final double center,
      final double startSigma, final double startHeight, final double minHeight) {
    final double xScale = startSigma;
    double yScale = minHeight;
    for (final double v : intensities) {
      yScale = Math.max(yScale, v);
    }
    if (yScale <= 0) {
      return null;
    }
    final int n = mzs.length;
    final double[] x = new double[n];
    final double[] y = new double[n];
    for (int i = 0; i < n; i++) {
      x[i] = (mzs[i] - center) / xScale;
      y[i] = intensities[i] / yScale;
    }
    final double minH = minHeight / yScale;

    // parameters: height, mu, sigma, tau. The Gaussian center sits left of the apex of a tailing
    // peak, start with half of the width as sigma and tail
    final double[] start = {Math.max(startHeight / yScale, minH), -0.5, 0.6, 1};
    final double[] p = WeightedLeastSquares.fit(y, weights, params -> {
      final double maxValue = EmgMath.shape(EmgMath.mode(params[1], params[2], params[3]),
          params[1], params[2], params[3]);
      final double[] values = new double[n];
      for (int i = 0; i < n; i++) {
        values[i] = maxValue > 0 ? params[0] * EmgMath.shape(x[i], params[1], params[2], params[3])
            / maxValue : 0;
      }
      return values;
    }, start, params -> {
      // decision: saturation only lowers the apex, so the true apex is at least the raw apex
      params[0] = Math.max(params[0], minH);
      params[1] = Math.max(-10, Math.min(10, params[1]));
      params[2] = Math.max(1e-3, Math.abs(params[2]));
      params[3] = Math.max(1e-3, Math.abs(params[3]));
      return params;
    });
    if (p == null) {
      return null;
    }
    final double mode = EmgMath.mode(p[1], p[2], p[3]);
    return new Fit(p[0] * yScale, center + p[1] * xScale, p[2] * xScale, p[3] * xScale,
        center + mode * xScale);
  }
}
