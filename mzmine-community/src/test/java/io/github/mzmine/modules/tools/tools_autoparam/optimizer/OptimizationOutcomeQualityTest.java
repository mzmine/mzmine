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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer;

import io.github.mzmine.modules.tools.tools_autoparam.optimizer.search.SolutionOrigin;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moeaframework.core.Solution;
import org.moeaframework.core.objective.Maximize;
import org.moeaframework.core.objective.Minimize;

class OptimizationOutcomeQualityTest {

  @Test
  void acceptsAnImprovedMaximizationResult() {
    final Solution estimate = solution(4d, true, 10);
    final Solution chosen = solution(6d, true, 12);

    OptimizationOutcomeQuality.assertQuality(estimate, List.of(chosen), List.of(estimate, chosen));
  }

  @Test
  void acceptsAnImprovedMinimizationResult() {
    final Solution estimate = solution(10d, false, 10);
    final Solution chosen = solution(8d, false, 12);

    OptimizationOutcomeQuality.assertQuality(estimate, List.of(chosen), List.of(estimate, chosen));
  }

  @Test
  void rejectsARegressedChosenScore() {
    final Solution estimate = solution(6d, true, 10);
    final Solution chosen = solution(4d, true, 12);

    Assertions.assertThrows(AssertionError.class,
        () -> OptimizationOutcomeQuality.assertQuality(estimate, List.of(chosen),
            List.of(estimate, chosen)));
  }

  @Test
  void rejectsARegressedChosenScoreWhenEstimateIsNotInEvaluatedCandidates() {
    final Solution estimate = solution(6d, true, 10);
    final Solution chosen = solution(4d, true, 12);

    Assertions.assertThrows(AssertionError.class,
        () -> OptimizationOutcomeQuality.assertQuality(estimate, List.of(chosen), List.of(chosen)));
  }

  @Test
  void rejectsInvalidOrEmptyReturnedResults() {
    final Solution estimate = solution(4d, true, 10);
    final Solution nonFinite = solution(Double.NaN, true, 12);
    final Solution empty = solution(6d, true, 0);

    Assertions.assertThrows(AssertionError.class,
        () -> OptimizationOutcomeQuality.assertQuality(estimate, List.of(nonFinite),
            List.of(estimate, nonFinite)));
    Assertions.assertThrows(AssertionError.class,
        () -> OptimizationOutcomeQuality.assertQuality(estimate, List.of(empty),
            List.of(estimate, empty)));
    Assertions.assertThrows(AssertionError.class,
        () -> OptimizationOutcomeQuality.assertQuality(estimate, List.of(), List.of(estimate)));
  }

  @Test
  void excludesCurrentBaselineFromTheSearchComparison() {
    final Solution estimate = solution(6d, true, 10);
    final Solution chosen = solution(6d, true, 12);
    final Solution current = solution(9d, true, 14);
    SolutionOrigin.CURRENT.applyTo(current);

    OptimizationOutcomeQuality.assertQuality(estimate, List.of(chosen),
        List.of(estimate, chosen, current));
  }

  @Test
  void rejectsCurrentBaselineInTheReturnedFront() {
    final Solution estimate = solution(6d, true, 10);
    final Solution current = solution(9d, true, 14);
    SolutionOrigin.CURRENT.applyTo(current);

    Assertions.assertThrows(AssertionError.class,
        () -> OptimizationOutcomeQuality.assertQuality(estimate, List.of(current),
            List.of(estimate)));
  }

  private static @NotNull Solution solution(final double score, final boolean maximize,
      final int totalFeatures) {
    final Solution solution = new Solution(0, 1);
    solution.setObjective(0, maximize ? new Maximize("Score") : new Minimize("Score"));
    solution.setObjectiveValue(0, score);
    solution.setAttribute("Total features", totalFeatures);
    return solution;
  }
}
