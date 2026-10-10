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
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moeaframework.core.Solution;
import org.moeaframework.core.constraint.LessThanOrEqual;
import org.moeaframework.core.objective.Maximize;
import org.moeaframework.core.objective.Minimize;

class OptimizationProgressDataTest {

  @Test
  void createsMaximizationStepLineAndIgnoresInfeasibleImprovements() {
    final Solution estimate = solution(1, 3d, true, true);
    final Solution infeasible = solution(2, 5d, false, true);
    final Solution improved = solution(3, 4d, true, true);
    final Solution best = solution(4, 6d, true, true);

    final OptimizationProgressData data = OptimizationProgressData.create(
        List.of(best, infeasible, estimate, improved), estimate, 0);

    Assertions.assertArrayEquals(new double[]{1d, 2d, 3d, 4d}, data.evaluations());
    Assertions.assertArrayEquals(new double[]{3d, 5d, 4d, 6d}, data.scores());
    Assertions.assertArrayEquals(new double[]{1d, 2d, 3d, 3d, 4d, 4d}, data.bestEvaluations());
    Assertions.assertArrayEquals(new double[]{3d, 3d, 3d, 4d, 4d, 6d}, data.bestScores());
    Assertions.assertEquals(3d, data.estimateScore());
    Assertions.assertEquals("Score", data.objectiveName());
  }

  @Test
  void respectsMinimizationDirection() {
    final Solution first = solution(1, 10d, true, false);
    final Solution best = solution(2, 8d, true, false);
    final Solution worse = solution(3, 9d, true, false);

    final OptimizationProgressData data = OptimizationProgressData.create(
        List.of(first, best, worse), first, 0);

    Assertions.assertArrayEquals(new double[]{1d, 2d, 2d, 3d}, data.bestEvaluations());
    Assertions.assertArrayEquals(new double[]{10d, 10d, 8d, 8d}, data.bestScores());
  }

  private static @NotNull Solution solution(int evaluation, double score, boolean feasible,
      boolean maximize) {
    final Solution solution = new Solution(0, 1, 1);
    solution.setObjective(0, maximize ? new Maximize("Score") : new Minimize("Score"));
    solution.setObjectiveValue(0, score);
    solution.setConstraint(0, new LessThanOrEqual("Shape rejection", 1d));
    solution.setConstraintValue(0, feasible ? 0d : 2d);
    solution.setAttribute(WizardOptimizationProblem.ATTR_PROPOSAL_INDEX, evaluation);
    return solution;
  }
}
