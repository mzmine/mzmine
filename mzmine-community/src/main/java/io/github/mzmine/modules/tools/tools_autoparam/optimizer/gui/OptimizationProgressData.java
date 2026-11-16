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

package io.github.mzmine.modules.tools.tools_autoparam.optimizer.gui;

import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.WizardOptimizationProblem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moeaframework.core.Solution;

/**
 * Snapshot of chart values derived from completed optimizer evaluations.
 */
public record OptimizationProgressData(double @NotNull [] evaluations, double @NotNull [] scores,
                                       double @NotNull [] bestEvaluations,
                                       double @NotNull [] bestScores, double estimateScore,
                                       @NotNull String objectiveName) {

  public static @NotNull OptimizationProgressData create(@NotNull List<Solution> solutions,
      @Nullable Solution estimate, int objectiveIndex) {
    if (solutions.isEmpty()) {
      return new OptimizationProgressData(new double[0], new double[0], new double[0],
          new double[0], Double.NaN, "Score");
    }

    final Map<Solution, Integer> originalOrder = new IdentityHashMap<>(solutions.size());
    for (int i = 0; i < solutions.size(); i++) {
      originalOrder.put(solutions.get(i), i + 1);
    }
    final List<Solution> ordered = new ArrayList<>(solutions);
    ordered.sort(Comparator.comparingDouble(
        solution -> evaluationNumber(solution, originalOrder.get(solution))));

    final List<Double> evaluationValues = new ArrayList<>(ordered.size());
    final List<Double> scoreValues = new ArrayList<>(ordered.size());
    final List<Double> bestEvaluationValues = new ArrayList<>(ordered.size() * 2);
    final List<Double> bestScoreValues = new ArrayList<>(ordered.size() * 2);

    double bestCanonical = Double.POSITIVE_INFINITY;
    double bestRaw = Double.NaN;
    for (final Solution solution : ordered) {
      if (objectiveIndex < 0 || objectiveIndex >= solution.getNumberOfObjectives()) {
        continue;
      }
      final double score = solution.getObjectiveValue(objectiveIndex);
      if (!Double.isFinite(score)) {
        continue;
      }
      final double evaluation = evaluationNumber(solution, originalOrder.get(solution));
      evaluationValues.add(evaluation);
      scoreValues.add(score);

      final double canonical = solution.getObjective(objectiveIndex).getCanonicalValue();
      final boolean improved =
          solution.isFeasible() && Double.isFinite(canonical) && canonical < bestCanonical;
      if (improved && Double.isFinite(bestRaw)) {
        // decision: repeat the x value so a regular colored line renderer draws a true step.
        bestEvaluationValues.add(evaluation);
        bestScoreValues.add(bestRaw);
      }
      if (improved) {
        bestCanonical = canonical;
        bestRaw = score;
      }
      if (Double.isFinite(bestRaw)) {
        bestEvaluationValues.add(evaluation);
        bestScoreValues.add(bestRaw);
      }
    }

    final String objectiveName =
        objectiveIndex >= 0 && objectiveIndex < solutions.getFirst().getNumberOfObjectives()
            ? solutions.getFirst().getObjective(objectiveIndex).getName() : "Score";
    final double estimateScore =
        estimate != null && objectiveIndex >= 0 && objectiveIndex < estimate.getNumberOfObjectives()
            ? estimate.getObjectiveValue(objectiveIndex) : Double.NaN;
    return new OptimizationProgressData(toArray(evaluationValues), toArray(scoreValues),
        toArray(bestEvaluationValues), toArray(bestScoreValues), estimateScore, objectiveName);
  }

  private static double evaluationNumber(@NotNull Solution solution, int fallback) {
    final Object value = solution.getAttribute(WizardOptimizationProblem.ATTR_PROPOSAL_INDEX);
    return value instanceof Number number ? number.doubleValue() : fallback;
  }

  private static double @NotNull [] toArray(@NotNull List<Double> values) {
    return values.stream().mapToDouble(Double::doubleValue).toArray();
  }
}
