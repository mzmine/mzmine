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

import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.GroundTruthEvaluator.Summary;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.SyntheticLcmsData.Ion;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MzToleranceEstimationTest {

  private static final int MIN_CONSECUTIVE = 5;
  private static final double MIN_GROUP_INTENSITY = 1E3;
  private static final double MIN_HEIGHT = 1E4;

  /**
   * Random ions with the given m/z error at the apex, weak signals scatter up to 3 times more.
   */
  @NotNull
  private static SyntheticLcmsData ions(int numIons, double apexErrorPpm, long seed) {
    return ions(numIons, apexErrorPpm, seed, false);
  }

  /**
   * @param withoutNoiseFilter adds dense weak noise below half the min group intensity and keeps
   *                           the weak ion signals, like data without noise filter in the mass
   *                           detection
   */
  @NotNull
  private static SyntheticLcmsData ions(int numIons, double apexErrorPpm, long seed,
      boolean withoutNoiseFilter) {
    final int numScans = 600;
    final Random random = new Random(seed);
    final List<Ion> ions = new ArrayList<>();
    for (int i = 0; i < numIons; i++) {
      final double mz = 100 + random.nextDouble() * 900;
      final double apex = 10 + random.nextDouble() * (numScans - 20);
      final double sigma = 2 + random.nextDouble() * 4;
      final double height = Math.exp(Math.log(5E3) + random.nextDouble() * Math.log(1E3));
      ions.add(new Ion(mz, apex, sigma, height, apexErrorPpm, 0));
    }
    final SyntheticLcmsData.Builder builder = SyntheticLcmsData.builder(numScans).ions(ions)
        .noise(150, 100, 1000, 1E2, 3E3).maxErrorFactor(3).seed(seed);
    if (withoutNoiseFilter) {
      builder.additionalNoise(3000, 10, 400);
    } else {
      builder.detectionThreshold(5E2);
    }
    return builder.build();
  }

  @Nullable
  private static MzToleranceEstimate estimate(@NotNull SyntheticLcmsData data) {
    return MzToleranceEstimation.estimate(List.of(data.scans()), MIN_CONSECUTIVE,
        MIN_GROUP_INTENSITY, MIN_HEIGHT, null);
  }

  @Test
  void estimateFollowsTheMzErrorOfTheInstrument() {
    final MzToleranceEstimate precise = estimate(ions(1500, 1, 1));
    final MzToleranceEstimate scattered = estimate(ions(1500, 3, 1));
    Assertions.assertNotNull(precise);
    Assertions.assertNotNull(scattered);
    final double precisePpm = precise.tolerance().getPpmTolerance();
    // the scatter of a data point is 1 to 3 ppm, the requirement covers 99.5% of it
    final double requiredPpm = precise.scatterTolerance().getPpmTolerance();
    Assertions.assertTrue(requiredPpm >= 3 && requiredPpm <= 10, precise.toString());
    Assertions.assertTrue(precisePpm >= MzToleranceEstimation.SAFETY_FACTOR * requiredPpm - 0.01,
        precise.toString());
    final double ratio = scattered.tolerance().getPpmTolerance() / precisePpm;
    Assertions.assertTrue(ratio > 2.3 && ratio < 3.7, "ratio " + ratio);
  }

  @Test
  void estimatedToleranceFindsTheIons() {
    final SyntheticLcmsData data = ions(1500, 2, 5);
    final MzToleranceEstimate estimate = estimate(data);
    Assertions.assertNotNull(estimate);
    final List<BuiltChromatogram> chromatograms = new FastChromatogramBuilder(estimate.tolerance(),
        MIN_CONSECUTIVE, MIN_GROUP_INTENSITY, MIN_HEIGHT).build(data.scans(), null, null);
    Assertions.assertNotNull(chromatograms);
    final Summary summary = GroundTruthEvaluator.evaluate(data,
        EvaluatedChromatogram.of(chromatograms), MIN_CONSECUTIVE, MIN_GROUP_INTENSITY, MIN_HEIGHT);
    Assertions.assertTrue(summary.foundIons() >= summary.detectableIons() * 0.99,
        summary.toString());
    Assertions.assertTrue(summary.meanCompleteness() > 0.98, summary.toString());
    Assertions.assertTrue(summary.splitIons() <= summary.detectableIons() * 0.01,
        summary.toString());
  }

  @Test
  void estimateIsDeterministic() {
    final MzToleranceEstimate first = estimate(ions(800, 2, 9));
    final MzToleranceEstimate second = estimate(ions(800, 2, 9));
    Assertions.assertNotNull(first);
    Assertions.assertNotNull(second);
    Assertions.assertEquals(first.tolerance(), second.tolerance());
  }

  @Test
  void onlyNoiseGivesNoEstimate() {
    Assertions.assertNull(estimate(ions(0, 2, 3)));
  }

  /**
   * Noise dominates the pairs of consecutive scans, the estimate uses the data points above an
   * intensity floor and finds the scatter of the same ions with a noise filter.
   */
  @Test
  void denseWeakNoiseUsesAnIntensityFloor() {
    final MzToleranceEstimate filtered = estimate(ions(1500, 2, 11, false));
    final MzToleranceEstimate unfiltered = estimate(ions(1500, 2, 11, true));
    Assertions.assertNotNull(filtered);
    Assertions.assertNotNull(unfiltered);
    Assertions.assertEquals(0d, filtered.intensityFloor());
    Assertions.assertEquals(MIN_GROUP_INTENSITY / 2, unfiltered.intensityFloor(),
        unfiltered.toString());
    final double ratio =
        unfiltered.tolerance().getPpmTolerance() / filtered.tolerance().getPpmTolerance();
    Assertions.assertTrue(ratio > 0.8 && ratio < 1.25,
        "filtered %s, unfiltered %s".formatted(filtered, unfiltered));
  }

  @Test
  void intensityFloorsStartWithAllDataPoints() {
    Assertions.assertArrayEquals(new double[]{0, 500, 1000},
        MzToleranceEstimation.intensityFloors(1000));
    Assertions.assertArrayEquals(new double[]{0}, MzToleranceEstimation.intensityFloors(0));
  }

  @Test
  void tooFewScansGiveNoEstimate() {
    final SyntheticLcmsData data = SyntheticLcmsData.builder(3).ion(new Ion(300, 1, 2, 1E5, 2, 0))
        .build();
    Assertions.assertNull(estimate(data));
  }

  @Test
  void roundsUpToReadableValues() {
    Assertions.assertEquals(new MZTolerance(0.0013, 7.3),
        MzToleranceEstimation.roundUp(new MZTolerance(0.00121, 7.21)));
    Assertions.assertEquals(new MZTolerance(0.002, 10),
        MzToleranceEstimation.roundUp(new MZTolerance(0.002, 10)));
  }
}
