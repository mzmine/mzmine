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
import org.moeaframework.core.Solution;

/**
 * Test-only quality guard for real-data optimizer benchmarks.
 */
final class OptimizationOutcomeQuality {

  private static final String TOTAL_FEATURES = "Total features";

  private OptimizationOutcomeQuality() {
  }

  static void assertQuality(final @NotNull OptimizationOutcome outcome) {
    assertQuality(outcome.estimateSolution(), outcome.front().asList(), outcome.evaluatedSolutions());
  }

  static void assertQuality(final @NotNull Solution estimate, final @NotNull List<Solution> front,
      final @NotNull List<Solution> evaluated) {
    assertResult(estimate, "estimator baseline");
    Assertions.assertFalse(front.isEmpty(), "optimization returned an empty front");
    front.forEach(solution -> {
      Assertions.assertTrue(solution.isFeasible(), "front contains an infeasible solution");
      Assertions.assertNotEquals(SolutionOrigin.CURRENT, SolutionOrigin.of(solution),
          "front contains the Current baseline");
      assertResult(solution, "front solution");
    });

    final Solution chosen = front.stream().min(OptimizationOutcomeQuality::compareCanonical)
        .orElseThrow();
    if (isEligible(estimate)) {
      Assertions.assertTrue(isNoWorse(chosen, estimate),
          "chosen front score is worse than the feasible estimator baseline");
    }
    final Solution bestCandidate = evaluated.stream().filter(OptimizationOutcomeQuality::isEligible)
        .min(OptimizationOutcomeQuality::compareCanonical).orElse(null);
    Assertions.assertTrue(bestCandidate != null || isEligible(estimate),
        "no feasible estimator baseline or completed candidate is available");
    if (bestCandidate == null) {
      return;
    }
    Assertions.assertTrue(isNoWorse(chosen, bestCandidate),
        "chosen front score is worse than the best eligible completed candidate");
  }

  private static boolean isEligible(final @NotNull Solution solution) {
    return SolutionOrigin.of(solution) != SolutionOrigin.CURRENT && solution.isFeasible()
        && solution.getNumberOfObjectives() == 1 && hasFiniteScores(solution)
        && hasFeatures(solution);
  }

  private static void assertResult(final @NotNull Solution solution, final @NotNull String label) {
    Assertions.assertEquals(1, solution.getNumberOfObjectives(),
        label + " must have exactly one objective");
    Assertions.assertTrue(hasFiniteScores(solution), label + " has a non-finite objective score");
    Assertions.assertTrue(hasFeatures(solution), label + " has no meaningful Total features count");
  }

  private static boolean hasFiniteScores(final @NotNull Solution solution) {
    for (int index = 0; index < solution.getNumberOfObjectives(); index++) {
      if (!Double.isFinite(solution.getObjectiveValue(index))) {
        return false;
      }
    }
    return solution.getNumberOfObjectives() > 0;
  }

  private static boolean hasFeatures(final @NotNull Solution solution) {
    return solution.getAttribute(TOTAL_FEATURES) instanceof Number count
        && Double.isFinite(count.doubleValue()) && count.doubleValue() > 0d;
  }

  private static boolean isNoWorse(final @NotNull Solution candidate,
      final @NotNull Solution reference) {
    return compareCanonical(candidate, reference) <= 0;
  }

  private static int compareCanonical(final @NotNull Solution first,
      final @NotNull Solution second) {
    return first.getObjective(0).compareTo(second.getObjective(0));
  }
}
