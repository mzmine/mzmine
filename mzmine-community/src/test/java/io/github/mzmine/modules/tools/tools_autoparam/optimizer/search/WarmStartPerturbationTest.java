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

import io.github.mzmine.modules.tools.tools_autoparam.estimation.domain.SearchScale;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.RealVariable;

/**
 * Pins how {@link WarmStartInitialization#perturb} scales its jitter.
 * <p>
 * The property that matters: for a variable spanning orders of magnitude the jitter must be
 * relative to the estimate, not to the width of the box. With an absolute sigma the minimum height
 * was perturbed by several times its own estimate, so the warm start degenerated into a uniform
 * draw over the whole range - and it did so silently, producing plausible numbers that carried no
 * information about the estimate.
 */
public class WarmStartPerturbationTest {

  private static final int DRAWS = 4000;

  /**
   * Tolerances are loose because these are statistics over random draws; they only have to
   * distinguish relative from absolute scaling, which differ by orders of magnitude here.
   */
  private static final double RELATIVE_TOLERANCE = 0.25;

  private static @NotNull List<Double> draws(@NotNull RealVariable variable, double estimate) {
    final Solution solution = new Solution(1, 1);
    solution.setVariable(0, variable.copy());
    final SearchScale scale =
        "wide".equals(variable.getName()) ? SearchScale.LOGARITHMIC : SearchScale.LINEAR;

    return java.util.stream.IntStream.range(0, DRAWS).mapToObj(_ -> {
      ((RealVariable) solution.getVariable(0)).setValue(
          Math.clamp(estimate, variable.getLowerBound(), variable.getUpperBound()));
      WarmStartInitialization.perturb(solution, 0.20, _ -> scale);
      return RealVariable.getReal(solution.getVariable(0));
    }).toList();
  }

  /**
   * Standard deviation of the natural log of the draws, i.e. the relative spread.
   */
  private static double logSpread(@NotNull RealVariable variable, double estimate) {
    return standardDeviation(draws(variable, estimate).stream().map(Math::log).toList());
  }

  private static double absoluteSpread(@NotNull RealVariable variable, double estimate) {
    return standardDeviation(draws(variable, estimate));
  }

  private static double standardDeviation(@NotNull List<Double> values) {
    final double mean = values.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    return Math.sqrt(
        values.stream().mapToDouble(v -> (v - mean) * (v - mean)).average().orElseThrow());
  }

  @BeforeEach
  void seed() {
    PRNG.setSeed(42);
  }

  @Test
  @DisplayName("a logarithmic variable gets the same relative jitter whatever its upper bound")
  void wideRangeJitterIsIndependentOfBoxWidth() {
    // same estimate, boxes differing by a factor of 1000. Under absolute scaling the second would
    // be perturbed 1000x harder.
    final double narrowBoxSpread = logSpread(new RealVariable("wide", 1d, 1_000d), 100d);
    final double wideBoxSpread = logSpread(new RealVariable("wide", 1d, 1_000_000d), 100d);

    Assertions.assertEquals(narrowBoxSpread, wideBoxSpread, narrowBoxSpread * RELATIVE_TOLERANCE,
        "the relative jitter must not depend on how wide the box is, got %.3f vs %.3f".formatted(
            narrowBoxSpread, wideBoxSpread));
  }

  @Test
  @DisplayName("the relative jitter stays a sane multiple of the estimate")
  void relativeJitterIsAroundTheConfiguredFactor() {
    final RealVariable variable = new RealVariable("wide", 1d, 1_000_000d);
    final List<Double> values = draws(variable, 100d);

    final double median = values.stream().sorted().toList().get(values.size() / 2);
    Assertions.assertEquals(100d, median, 100d * RELATIVE_TOLERANCE,
        "the estimate must remain the centre of the distribution");

    // a factor rather than an absolute window, and comfortably inside the box
    final double low = values.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
    final double high = values.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
    Assertions.assertTrue(high / low < 1000d,
        "%.4g .. %.4g spans more than three decades, that is a uniform draw not a perturbation".formatted(
            low, high));
    Assertions.assertTrue(high / low > 4d,
        "%.4g .. %.4g is too tight to explore the estimate's neighbourhood".formatted(low, high));
  }

  @Test
  @DisplayName("a narrow range variable keeps its absolute, range proportional jitter")
  void narrowRangeStaysAdditive() {
    // an explicitly linear scale makes sigma a fraction of the range, so two different estimates
    // in the same box must scatter by the same absolute amount
    final double spreadHigh = absoluteSpread(new RealVariable("narrow", 0.5d, 1.5d), 1.2d);
    final double spreadLow = absoluteSpread(new RealVariable("narrow", 0.5d, 1.5d), 0.7d);

    Assertions.assertEquals(spreadHigh, spreadLow, spreadHigh * RELATIVE_TOLERANCE,
        "an additive variable must scatter by the same amount regardless of its estimate");
  }

  @Test
  @DisplayName("ordinal variables are never log scaled")
  void ordinalsStayAdditive() {
    // one ordinal step is the natural unit, irrespective of the declared continuous scale
    final RealVariable ordinal = new OrdinalIntegerVariable("ordinal", 2, 40);
    final double spread = absoluteSpread(ordinal, 5d);

    // absolute scaling on a range of 38 gives sigma near 7.6; a relative one would give near 2.5
    Assertions.assertTrue(spread > 4d,
        "an ordinal scattered by only %.2f, which means it was perturbed relative to its estimate".formatted(
            spread));
  }

  @Test
  @DisplayName("no perturbation ever leaves the variable's bounds")
  void perturbationsStayInBounds() {
    for (final RealVariable variable : List.of(new RealVariable("wide", 1d, 1_000_000d),
        new RealVariable("narrow", 0.5d, 1.5d), new OrdinalIntegerVariable("ordinal", 2, 40))) {
      // an estimate deliberately outside the box, which the clamp has to absorb
      for (final double estimate : new double[]{variable.getLowerBound() / 10,
          variable.getUpperBound() * 10}) {
        for (final double value : draws(variable, estimate)) {
          Assertions.assertTrue(
              value >= variable.getLowerBound() && value <= variable.getUpperBound(),
              "%s left its bounds: %.6g not in [%.6g, %.6g]".formatted(variable.getName(), value,
                  variable.getLowerBound(), variable.getUpperBound()));
        }
      }
    }
  }
}
