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

import org.apache.commons.math3.special.Erf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks the fast EMG math against the commons-math error function and a numeric maximum search,
 * which were used by the first EMG implementation.
 */
class EmgMathTest {

  @Test
  void erfcMatchesCommonsMath() {
    for (double z = -6; z <= 6; z += 0.01) {
      final double reference = Erf.erfc(z);
      Assertions.assertEquals(reference, EmgMath.erfc(z), 2e-7 * reference + 1e-300, "z=" + z);
    }
  }

  @Test
  void erfcxMatchesCommonsMath() {
    for (double z = -4; z <= 5; z += 0.01) {
      final double reference = Math.exp(z * z) * Erf.erfc(z);
      Assertions.assertEquals(reference, EmgMath.erfcx(z), 2e-7 * reference, "z=" + z);
    }
    // large z (far left flank, negligible density): no overflow, approaches 1 / (z sqrt(pi))
    final double asymptote = 1 / (50 * Math.sqrt(Math.PI));
    Assertions.assertEquals(asymptote, EmgMath.erfcx(50), 1e-3 * asymptote);
  }

  /**
   * tau / sigma ratios from nearly Gaussian to extremely tailing. The fitted profile peaks have
   * ratios of about 2-5.
   */
  @ParameterizedTest
  @ValueSource(doubles = {0.05, 0.1, 0.3, 0.5, 0.8, 1, 1.25, 1.5, 2, 3, 4, 5, 8, 12, 20})
  void closedFormModeMatchesNumericMaximum(final double tauOverSigma) {
    final double mu = 0.3;
    final double sigma = 0.7;
    final double tau = tauOverSigma * sigma;
    final double reference = goldenSectionMode(mu, sigma, tau);
    Assertions.assertEquals(reference, EmgMath.mode(mu, sigma, tau), 1e-4 * sigma,
        "tau/sigma=" + tauOverSigma);
  }

  private static double referenceShape(final double x, final double mu, final double sigma,
      final double tau) {
    final double d = x - mu;
    final double z = (sigma / tau - d / sigma) / Math.sqrt(2);
    if (z < 0) {
      return Math.exp(sigma * sigma / (2 * tau * tau) - d / tau) * Erf.erfc(z) / (2 * tau);
    }
    return Math.exp(-d * d / (2 * sigma * sigma)) * Math.exp(z * z) * Erf.erfc(z) / (2 * tau);
  }

  private static double goldenSectionMode(final double mu, final double sigma, final double tau) {
    double a = mu - 2 * sigma;
    double b = mu + tau + sigma;
    final double phi = (Math.sqrt(5) - 1) / 2;
    double c = b - phi * (b - a);
    double d = a + phi * (b - a);
    for (int i = 0; i < 200; i++) {
      if (referenceShape(c, mu, sigma, tau) > referenceShape(d, mu, sigma, tau)) {
        b = d;
      } else {
        a = c;
      }
      c = b - phi * (b - a);
      d = a + phi * (b - a);
    }
    return (a + b) / 2;
  }
}
