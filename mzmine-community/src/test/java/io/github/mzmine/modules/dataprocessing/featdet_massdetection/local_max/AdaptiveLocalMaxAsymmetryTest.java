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

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The asymmetric peak decision (apex parabola vs weighted centroid) of the
 * {@link AdaptiveLocalMaxMassDetector} must not depend on smoothing. The edges of a peak on a noisy
 * baseline move with smoothing, the peak shape does not.
 */
class AdaptiveLocalMaxAsymmetryTest {

  private static final double APEX_MZ = 500.0;

  /**
   * Tailing profile peak (left Gaussian half width 0.008, right 0.024) of height 500k on a noisy
   * baseline of a few hundred counts with small dips, TOF grid with ~0.004 spacing and zero end
   * points.
   */
  private static @NotNull SimpleMassSpectrum tailingPeakOnNoisyBaseline() {
    final int n = 301;
    final double[] mzs = new double[n];
    final double[] intensities = new double[n];
    final double sqrtStart = Math.sqrt(499.4);
    final double sqrtStep = 0.004 / (2 * Math.sqrt(APEX_MZ));
    for (int i = 0; i < n; i++) {
      final double mz = Math.pow(sqrtStart + i * sqrtStep, 2);
      final double delta = mz - APEX_MZ;
      final double width = delta < 0 ? 0.008 : 0.024;
      mzs[i] = mz;
      final double baseline = 400 + 200 * Math.sin(i * 1.7);
      intensities[i] = baseline + 500_000 * Math.exp(-0.5 * (delta / width) * (delta / width));
    }
    intensities[0] = 0;
    intensities[n - 1] = 0;
    return new SimpleMassSpectrum(mzs, intensities, MassSpectrumType.PROFILE);
  }

  private static double centroidNearApex(@NotNull final AdaptiveLocalMaxMassDetector detector,
      @NotNull final SimpleMassSpectrum spectrum) {
    final double[][] values = detector.getMassValues(spectrum);
    double best = Double.NaN;
    double bestIntensity = 0;
    for (int i = 0; i < values[0].length; i++) {
      if (values[1][i] > bestIntensity) {
        bestIntensity = values[1][i];
        best = values[0][i];
      }
    }
    return best;
  }

  private static double ppm(final double mz) {
    return (mz - APEX_MZ) / APEX_MZ * 1e6;
  }

  @Test
  void smoothingDoesNotChangeTheAsymmetricPeakCentroid() {
    final SimpleMassSpectrum spectrum = tailingPeakOnNoisyBaseline();
    final double raw = centroidNearApex(
        new AdaptiveLocalMaxMassDetector(0, AbundanceMeasure.Height, 3,
            new LocalMaxNoSmoothingModule()), spectrum);
    final double smoothed = centroidNearApex(
        new AdaptiveLocalMaxMassDetector(0, AbundanceMeasure.Height, 3,
            new LocalMaxGaussianModule(7)), spectrum);

    // smoothing removes the baseline dips, the edges move far into the baseline on both sides. The
    // point counts between the edges then look symmetric and the weighted centroid (~20 ppm in the
    // tail) was used instead of the apex parabola (~5.6 ppm).
    Assertions.assertEquals(ppm(raw), ppm(smoothed), 0.01,
        "raw %.3f ppm vs smoothed %.3f ppm".formatted(ppm(raw), ppm(smoothed)));
    Assertions.assertTrue(Math.abs(ppm(smoothed)) < 10, "%.3f ppm".formatted(ppm(smoothed)));
  }

  @Test
  void symmetricPeakKeepsTheWeightedCentroid() {
    final int n = 101;
    final double[] mzs = new double[n];
    final double[] intensities = new double[n];
    // TOF grid, a uniform grid is split into ranges by the detector
    final double sqrtStart = Math.sqrt(APEX_MZ - 0.1);
    final double sqrtStep = 0.002 / (2 * Math.sqrt(APEX_MZ));
    for (int i = 0; i < n; i++) {
      mzs[i] = Math.pow(sqrtStart + i * sqrtStep, 2);
      final double delta = mzs[i] - APEX_MZ - 0.0007;
      intensities[i] = 100_000 * Math.exp(-0.5 * (delta / 0.01) * (delta / 0.01));
    }
    intensities[0] = 0;
    intensities[n - 1] = 0;
    final double mz = centroidNearApex(
        new AdaptiveLocalMaxMassDetector(0, AbundanceMeasure.Height, 3,
            new LocalMaxNoSmoothingModule()),
        new SimpleMassSpectrum(mzs, intensities, MassSpectrumType.PROFILE));
    // weighted centroid of a symmetric peak sampled off-center is close to the true center
    Assertions.assertEquals(APEX_MZ + 0.0007, mz, 0.0002);
  }
}
