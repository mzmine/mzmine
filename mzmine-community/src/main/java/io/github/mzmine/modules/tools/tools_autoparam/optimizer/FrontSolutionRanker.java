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

import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.WizardOptimizationProblem;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.SweepMetric;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moeaframework.core.Solution;

/**
 * Selects the best solution of a non-dominated front, the same way for the results window and the
 * log. With several objectives, this is a deterministic compromise: the best average rank across
 * all objectives.
 */
public final class FrontSolutionRanker {

  private FrontSolutionRanker() {
  }

  /**
   * @return the best solution by average rank, ties are decided by
   * {@link #tieBreakObjectiveIndex(List)}. Null for an empty front.
   */
  public static @Nullable Solution selectBest(@NotNull List<Solution> front,
      @NotNull List<SweepMetric> metrics) {
    return selectBestAverageRank(front, tieBreakObjectiveIndex(metrics));
  }

  /**
   * @return the index of the metric that is {@link SweepMetric#preferredForRanking()}, otherwise
   * the first objective. Also the column the results are sorted by.
   */
  public static int tieBreakObjectiveIndex(@NotNull List<SweepMetric> metrics) {
    for (int i = 0; i < metrics.size(); i++) {
      if (metrics.get(i).preferredForRanking()) {
        return i;
      }
    }
    return 0;
  }

  static @Nullable Solution selectBestAverageRank(@NotNull List<Solution> front,
      int tieBreakObjectiveIndex) {
    if (front.isEmpty()) {
      return null;
    }

    return front.stream().min(
        Comparator.<Solution>comparingDouble(solution -> averageRank(solution, front))
            .thenComparingDouble(solution -> canonicalValue(solution, tieBreakObjectiveIndex))
            .thenComparingInt(FrontSolutionRanker::evaluationIndex)).orElse(null);
  }

  static double averageRank(@NotNull Solution candidate, @NotNull List<Solution> front) {
    final int objectives = candidate.getNumberOfObjectives();
    if (objectives == 0) {
      return 1d;
    }

    double rankSum = 0d;
    for (int objectiveIndex = 0; objectiveIndex < objectives; objectiveIndex++) {
      rankSum += rank(candidate, front, objectiveIndex);
    }
    return rankSum / objectives;
  }

  private static double rank(@NotNull Solution candidate, @NotNull List<Solution> front,
      int objectiveIndex) {
    final double candidateValue = canonicalValue(candidate, objectiveIndex);
    if (!Double.isFinite(candidateValue)) {
      return front.size() + 1d;
    }

    int better = 0;
    int tied = 0;
    for (final Solution other : front) {
      final double otherValue = canonicalValue(other, objectiveIndex);
      if (!Double.isFinite(otherValue)) {
        continue;
      }
      final int comparison = Double.compare(otherValue, candidateValue);
      if (comparison < 0) {
        better++;
      } else if (comparison == 0) {
        tied++;
      }
    }
    // Standard midrank gives tied scores the mean of the positions they occupy.
    return 1d + better + Math.max(0, tied - 1) / 2d;
  }

  private static double canonicalValue(@NotNull Solution solution, int objectiveIndex) {
    if (objectiveIndex < 0 || objectiveIndex >= solution.getNumberOfObjectives()) {
      return Double.POSITIVE_INFINITY;
    }
    return solution.getObjective(objectiveIndex).getCanonicalValue();
  }

  private static int evaluationIndex(@NotNull Solution solution) {
    final Object value = solution.getAttribute(WizardOptimizationProblem.ATTR_PROPOSAL_INDEX);
    return value instanceof Number number ? number.intValue() : Integer.MAX_VALUE;
  }
}
