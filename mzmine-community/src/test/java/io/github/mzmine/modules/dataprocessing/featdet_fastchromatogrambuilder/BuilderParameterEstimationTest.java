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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ChromatogramBuilderSettings;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.SignalPersistenceProfile.IntensityBin;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.SyntheticLcmsData.Ion;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import it.unimi.dsi.fastutil.ints.IntArrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BuilderParameterEstimationTest {

  private static final MZTolerance FALLBACK = new MZTolerance(0.002, 10);
  private static final double NOISE_MAX_INTENSITY = 3E3;

  /**
   * Random ions of 5E3 to 5E6 with a gaussian sigma of 2 to 6 scans, a full width at half maximum
   * of 5 to 14 scans, and uniform noise of 1E2 to 3E3 in every scan.
   *
   * @param withoutNoiseFilter adds dense weak noise and keeps the weak ion signals, like data
   *                           without noise filter in the mass detection
   */
  @NotNull
  private static SyntheticLcmsData data(boolean withoutNoiseFilter) {
    final int numScans = 600;
    final Random random = new Random(7);
    final List<Ion> ions = new ArrayList<>();
    for (int i = 0; i < 800; i++) {
      final double mz = 100 + random.nextDouble() * 900;
      final double apex = 10 + random.nextDouble() * (numScans - 20);
      final double sigma = 2 + random.nextDouble() * 4;
      final double height = Math.exp(Math.log(5E3) + random.nextDouble() * Math.log(1E3));
      ions.add(new Ion(mz, apex, sigma, height, 2, 0));
    }
    final SyntheticLcmsData.Builder builder = SyntheticLcmsData.builder(numScans).ions(ions)
        .noise(150, 100, 1000, 1E2, NOISE_MAX_INTENSITY).maxErrorFactor(3).seed(11);
    if (withoutNoiseFilter) {
      builder.additionalNoise(3000, 10, 400);
    } else {
      builder.detectionThreshold(5E2);
    }
    return builder.build();
  }

  @NotNull
  private static BuilderParameterEstimate estimate(@NotNull SyntheticLcmsData data,
      @NotNull ChromatogramBuilderSensitivity sensitivity) {
    return Objects.requireNonNull(
        BuilderParameterEstimation.estimate(List.of(data.scans()), sensitivity, FALLBACK, null));
  }

  @Test
  void levelsSeparateTheNoiseFromTheIons() {
    final BuilderParameterEstimate estimate = estimate(data(false),
        ChromatogramBuilderSensitivity.MEDIUM);
    Assertions.assertTrue(estimate.levelsFound(), estimate.toString());
    // dense noise up to 3E3 outnumbers the ion signals, the ions above it continue
    Assertions.assertTrue(estimate.noiseLevel() >= 1E3 && estimate.noiseLevel() < 5E3,
        estimate.toString());
    Assertions.assertTrue(
        estimate.signalLevel() >= estimate.noiseLevel() && estimate.signalLevel() < 1E4,
        estimate.toString());
    final ChromatogramBuilderSettings settings = estimate.settings();
    // weak signals scatter up to 6 ppm
    Assertions.assertTrue(settings.mzTolerance().getPpmTolerance() < 30, settings.toString());
    Assertions.assertNotNull(estimate.toleranceEstimate());
  }

  @Test
  void peakWidthOfTheClearPeaks() {
    final BuilderParameterEstimate estimate = estimate(data(false),
        ChromatogramBuilderSensitivity.MEDIUM);
    Assertions.assertTrue(estimate.numWidthPeaks() >= BuilderParameterEstimation.MIN_WIDTH_PEAKS,
        estimate.toString());
    Assertions.assertTrue(estimate.peakWidthScans() >= 5 && estimate.peakWidthScans() <= 14,
        estimate.toString());
  }

  @Test
  void sensitivitiesAreOrdered() {
    final SyntheticLcmsData data = data(false);
    ChromatogramBuilderSettings previous = null;
    for (final ChromatogramBuilderSensitivity sensitivity : ChromatogramBuilderSensitivity.values()) {
      final ChromatogramBuilderSettings settings = estimate(data, sensitivity).settings();
      Assertions.assertTrue(settings.minHeight() >= settings.minGroupIntensity(),
          settings.toString());
      if (previous != null) {
        Assertions.assertTrue(settings.minGroupIntensity() > previous.minGroupIntensity(),
            settings + " vs " + previous);
        Assertions.assertTrue(settings.minHeight() > previous.minHeight(),
            settings + " vs " + previous);
        Assertions.assertTrue(settings.minConsecutiveScans() >= previous.minConsecutiveScans(),
            settings + " vs " + previous);
      }
      previous = settings;
    }
  }

  @Test
  void dataWithoutNoiseFilterKeepTheSignalLevel() {
    final BuilderParameterEstimate filtered = estimate(data(false),
        ChromatogramBuilderSensitivity.MEDIUM);
    final BuilderParameterEstimate unfiltered = estimate(data(true),
        ChromatogramBuilderSensitivity.MEDIUM);
    Assertions.assertTrue(unfiltered.levelsFound(), unfiltered.toString());
    final double ratio = unfiltered.signalLevel() / filtered.signalLevel();
    Assertions.assertTrue(ratio > 1 / 3d && ratio < 3, filtered + "\n" + unfiltered);
    Assertions.assertTrue(unfiltered.settings().mzTolerance().getPpmTolerance() < 30,
        unfiltered.toString());
  }

  @Test
  void profileTellsNoiseFromSignals() {
    final Random random = new Random(3);
    final int numScans = 200;
    final double[][] mzs = new double[numScans][];
    final double[][] intensities = new double[numScans][];
    for (int s = 0; s < numScans; s++) {
      final int numNoise = 500;
      final int numSignals = 100;
      final double[] scanMzs = new double[numNoise + numSignals];
      final double[] scanIntensities = new double[scanMzs.length];
      for (int i = 0; i < numNoise; i++) {
        scanMzs[i] = 100 + random.nextDouble() * 900;
        scanIntensities[i] = 100 + random.nextDouble() * 100;
      }
      // signals in every scan at fixed m/z, 1E4 to 2E4
      for (int i = 0; i < numSignals; i++) {
        scanMzs[numNoise + i] = 105.5 + 9 * i + random.nextGaussian() * 1E-4;
        scanIntensities[numNoise + i] = 1E4 * (1 + random.nextDouble());
      }
      final int[] order = ChannelConsolidation.identity(scanMzs.length);
      IntArrays.quickSort(order,
          (a, b) -> Double.compare(scanMzs[a], scanMzs[b]));
      mzs[s] = new double[scanMzs.length];
      intensities[s] = new double[scanMzs.length];
      for (int i = 0; i < order.length; i++) {
        mzs[s][i] = scanMzs[order[i]];
        intensities[s][i] = scanIntensities[order[i]];
      }
    }
    final SignalPersistenceProfile profile = new SignalPersistenceProfile(new MZTolerance(0, 10));
    profile.addScans(new ArrayScans(mzs, intensities), 1);
    for (final IntensityBin bin : profile.bins()) {
      if (bin.lowerEdge() < 300) {
        Assertions.assertTrue(bin.signalFraction() < 0.1, bin.toString());
      } else {
        Assertions.assertTrue(bin.signalFraction() > 0.95, bin.toString());
      }
    }
    final double noise = profile.intensityAtSignalFraction(0.5);
    Assertions.assertTrue(noise > 200 && noise <= 1E4, String.valueOf(noise));
  }

  @Test
  void roundsToTwoSignificantDigits() {
    Assertions.assertEquals(12000d, BuilderParameterEstimation.roundSignificant(12345d), 1E-6);
    Assertions.assertEquals(0.046d, BuilderParameterEstimation.roundSignificant(0.04567d), 1E-9);
    Assertions.assertEquals(990d, BuilderParameterEstimation.roundSignificant(987d), 1E-9);
    Assertions.assertEquals(0d, BuilderParameterEstimation.roundSignificant(0d));
    Assertions.assertEquals(0d, BuilderParameterEstimation.roundSignificant(Double.NaN));
  }

  @Test
  void minConsecutiveScansFollowThePeakWidth() {
    Assertions.assertEquals(BuilderParameterEstimation.MIN_CONSECUTIVE,
        BuilderParameterEstimation.minConsecutiveScans(ChromatogramBuilderSensitivity.SENSITIVE,
            2));
    Assertions.assertEquals(BuilderParameterEstimation.MAX_CONSECUTIVE,
        BuilderParameterEstimation.minConsecutiveScans(ChromatogramBuilderSensitivity.ABUNDANT,
            500));
    Assertions.assertEquals(5,
        BuilderParameterEstimation.minConsecutiveScans(ChromatogramBuilderSensitivity.MEDIUM, 10));
    // without peaks the values of the batch wizard
    Assertions.assertEquals(4,
        BuilderParameterEstimation.minConsecutiveScans(ChromatogramBuilderSensitivity.MEDIUM,
            Double.NaN));
  }
}
