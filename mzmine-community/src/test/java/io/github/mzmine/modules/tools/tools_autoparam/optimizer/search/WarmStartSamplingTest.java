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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer.search;

import java.util.Arrays;
import java.util.stream.IntStream;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.moeaframework.core.PRNG;

/**
 * Pins the property the space-filling warm starts exist for: coverage of the estimate's
 * neighbourhood must not depend on the random seed.
 * <p>
 * The warm start decides most of a run's score, and with independent draws the same data and
 * settings score up to 1.9 times differently across seeds. A design whose coverage still varied
 * with the seed would not fix that, and it would fail silently - the numbers would look reasonable
 * and the lottery would continue.
 * <p>
 * Coverage is asserted in quantile space rather than in standard deviations. A design that covers
 * evenly reaches further into the tails on purpose, which makes its widest gap in sigma
 * <em>larger</em> than that of independent draws huddled around the centre - measuring there would
 * reward the wrong behaviour.
 */
public class WarmStartSamplingTest {

  /**
   * The real case: twenty warm-start solutions, one of which is the unperturbed estimate, over the
   * eight parameters the wizard exposes.
   */
  private static final int SOLUTIONS = 19;
  private static final int VARIABLES = 8;

  private static final NormalDistribution STANDARD_NORMAL = new NormalDistribution(0, 1);

  private static double[][] withSeed(@NotNull WarmStartSampling sampling, long seed) {
    PRNG.setSeed(seed);
    return sampling.normalDeviates(SOLUTIONS, VARIABLES);
  }

  /**
   * One column back in quantile space, sorted. This is where "evenly covered" is defined.
   */
  private static double[] quantiles(@NotNull double[][] deviates, int column) {
    final double[] u = new double[deviates.length];
    for (int i = 0; i < deviates.length; i++) {
      u[i] = STANDARD_NORMAL.cumulativeProbability(deviates[i][column]);
    }
    Arrays.sort(u);
    return u;
  }

  /**
   * Widest stretch of the distribution no solution samples, over all variables.
   */
  private static double worstGap(@NotNull double[][] deviates) {
    double worst = 0;
    for (int j = 0; j < VARIABLES; j++) {
      final double[] u = quantiles(deviates, j);
      worst = Math.max(worst, u[0]);
      for (int i = 1; i < u.length; i++) {
        worst = Math.max(worst, u[i] - u[i - 1]);
      }
      worst = Math.max(worst, 1 - u[u.length - 1]);
    }
    return worst;
  }

  @Test
  @DisplayName("sobol does not consult the random generator at all")
  void sobolIsIdenticalRegardlessOfSeed() {
    Assertions.assertArrayEquals(withSeed(WarmStartSampling.SOBOL, 1L),
        withSeed(WarmStartSampling.SOBOL, 999_999L),
        "a Sobol sequence is deterministic, so two seeds must give the identical design");
  }

  @Test
  @DisplayName("latin hypercube fills every stratum whatever the seed")
  void latinHypercubeFillsEveryStratum() {
    // MOEA draws one point at random inside each stratum, so the values move with the seed but the
    // strata they land in must not - that is the coverage guarantee, and it is all LHS promises
    final double width = 1.0 / SOLUTIONS;
    for (final long seed : new long[]{1L, 999_999L}) {
      final double[][] deviates = withSeed(WarmStartSampling.LATIN_HYPERCUBE, seed);
      for (int j = 0; j < VARIABLES; j++) {
        final double[] u = quantiles(deviates, j);
        for (int i = 0; i < u.length; i++) {
          Assertions.assertTrue(u[i] >= i * width - 1e-6 && u[i] <= (i + 1) * width + 1e-6,
              "seed %d, variable %d: stratum %d holds %.4f, which is outside [%.4f, %.4f]".formatted(
                  seed, j, i, u[i], i * width, (i + 1) * width));
        }
      }
    }
  }

  @Test
  @DisplayName("only sobol is fully seed free; latin hypercube still moves within its strata")
  void latinHypercubeIsNotFullyDeterministic() {
    // recorded deliberately: LHS fixes the coverage but not the exact design, so it removes most of
    // the seed dependence and not all of it
    Assertions.assertFalse(Arrays.deepEquals(withSeed(WarmStartSampling.LATIN_HYPERCUBE, 1L),
            withSeed(WarmStartSampling.LATIN_HYPERCUBE, 999_999L)),
        "if this ever passes, MOEA stopped randomising inside the strata and LHS became as "
            + "reproducible as Sobol");
  }

  @Test
  @DisplayName("independent draws vary with the seed, so the tests above mean something")
  void gaussianVariesWithTheSeed() {
    Assertions.assertFalse(Arrays.deepEquals(withSeed(WarmStartSampling.GAUSSIAN, 1L),
        withSeed(WarmStartSampling.GAUSSIAN, 999_999L)));
  }

  @Test
  @DisplayName("a space-filling design leaves no wide unsampled stretch")
  void spaceFillingCoversMoreEvenlyThanIndependentDraws() {
    // averaged over seeds, because a single independent draw is sometimes lucky
    final double gaussian = IntStream.range(0, 40)
        .mapToDouble(s -> worstGap(withSeed(WarmStartSampling.GAUSSIAN, s))).average()
        .orElseThrow();
    final double lhs = worstGap(withSeed(WarmStartSampling.LATIN_HYPERCUBE, 1L));
    final double sobol = worstGap(withSeed(WarmStartSampling.SOBOL, 1L));

    Assertions.assertTrue(lhs < gaussian,
        "latin hypercube left %.1f %% of the distribution unsampled against %.1f %% for independent draws".formatted(
            100 * lhs, 100 * gaussian));
    Assertions.assertTrue(sobol < gaussian,
        "sobol left %.1f %% of the distribution unsampled against %.1f %% for independent draws".formatted(
            100 * sobol, 100 * gaussian));
  }

  @Test
  @DisplayName("no deviate is infinite, including sobol's first point")
  void deviatesStayFinite() {
    for (final WarmStartSampling sampling : WarmStartSampling.values()) {
      final double[][] deviates = withSeed(sampling, 7L);
      Assertions.assertEquals(SOLUTIONS, deviates.length);
      for (final double[] row : deviates) {
        Assertions.assertEquals(VARIABLES, row.length);
        for (final double v : row) {
          Assertions.assertTrue(Double.isFinite(v) && Math.abs(v) < 6,
              "%s produced %s, which the tail clamp should have prevented".formatted(sampling, v));
        }
      }
    }
  }
}
