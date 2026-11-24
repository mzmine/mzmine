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

/**
 * Fast special functions for the exponentially modified Gaussian. The complementary error function
 * uses the Chebyshev fit of Numerical Recipes (fractional error below 1.2e-7 everywhere), which
 * directly gives the scaled form erfcx(z) = exp(z^2) erfc(z) without overflow. The mode is solved
 * in closed form with a fixed number of Newton steps instead of a numeric maximum search.
 */
public final class EmgMath {

  private static final double SQRT2 = Math.sqrt(2);
  private static final double SQRT_2_OVER_PI = Math.sqrt(2 / Math.PI);
  private static final double TWO_OVER_SQRT_PI = 2 / Math.sqrt(Math.PI);
  /**
   * Newton steps for the inverse of erfcx. The start value is already close, 4 steps reach a mode
   * error below 1e-4 sigma for tau/sigma between 0.05 and 20 (see EmgMathTest).
   */
  private static final int MODE_NEWTON_STEPS = 4;

  private EmgMath() {
  }

  /**
   * Chebyshev polynomial part of erfc for t = 1 / (1 + |z| / 2): erfc(|z|) = t exp(-z^2 + p(t)).
   */
  private static double poly(final double t) {
    return -1.26551223 + t * (1.00002368 + t * (0.37409196 + t * (0.09678418 + t * (-0.18628806
        + t * (0.27886807 + t * (-1.13520398 + t * (1.48851587 + t * (-0.82215223
        + t * 0.17087277))))))));
  }

  public static double erfc(final double z) {
    final double t = 1 / (1 + 0.5 * Math.abs(z));
    final double ans = t * Math.exp(-z * z + poly(t));
    return z >= 0 ? ans : 2 - ans;
  }

  /**
   * Scaled complementary error function exp(z^2) erfc(z).
   */
  public static double erfcx(final double z) {
    final double t = 1 / (1 + 0.5 * Math.abs(z));
    final double scaled = t * Math.exp(poly(t));
    // erfc(-|z|) = 2 - erfc(|z|)
    return z >= 0 ? scaled : 2 * Math.exp(z * z) - scaled;
  }

  /**
   * Unnormalized EMG density with an exponential tail to higher x (tau > 0).
   */
  public static double shape(final double x, final double mu, final double sigma,
      final double tau) {
    final double d = x - mu;
    final double z = (sigma / tau - d / sigma) / SQRT2;
    if (z < 0) {
      // right of the mode, the direct form is stable
      return Math.exp(sigma * sigma / (2 * tau * tau) - d / tau) * erfc(z) / (2 * tau);
    }
    return Math.exp(-d * d / (2 * sigma * sigma)) * erfcx(z) / (2 * tau);
  }

  /**
   * Mode of the EMG. The derivative of the density is zero where erfcx(z) = tau / sigma *
   * sqrt(2/pi) with z = (sigma / tau - d / sigma) / sqrt(2), so mode = mu + sigma^2 / tau - sqrt(2)
   * sigma z. z starts from a closed-form approximation of the inverse of erfcx and is refined with
   * a fixed number of Newton steps.
   */
  public static double mode(final double mu, final double sigma, final double tau) {
    final double y = tau / sigma * SQRT_2_OVER_PI;
    double z;
    if (y >= 1) {
      // tailing (z <= 0): erfcx(z) ~ 2 exp(z^2) - 1, exact at z = 0 and asymptotically for z << 0
      z = -Math.sqrt(Math.log((y + 1) / 2));
    } else {
      // Abramowitz & Stegun 7.1.13 bound erfcx(z) ~ 2 / (sqrt(pi) (z + sqrt(z^2 + 4 / pi))),
      // exact at z = 0 and inverted in closed form
      final double k = TWO_OVER_SQRT_PI / y;
      z = (k * k - 4 / Math.PI) / (2 * k);
    }
    for (int i = 0; i < MODE_NEWTON_STEPS; i++) {
      final double value = erfcx(z);
      // d/dz erfcx(z) = 2 z erfcx(z) - 2 / sqrt(pi)
      final double derivative = 2 * z * value - TWO_OVER_SQRT_PI;
      if (derivative == 0) {
        break;
      }
      z -= (value - y) / derivative;
    }
    return mu + sigma * sigma / tau - SQRT2 * sigma * z;
  }
}
