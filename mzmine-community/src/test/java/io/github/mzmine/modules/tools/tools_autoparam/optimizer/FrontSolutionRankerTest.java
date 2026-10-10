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
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moeaframework.core.Solution;
import org.moeaframework.core.objective.Maximize;
import org.moeaframework.core.objective.Minimize;

class FrontSolutionRankerTest {

  @Test
  void selectsBestAverageRankAcrossMaximizeAndMinimizeObjectives() {
    final Solution first = solution(1, 10d, 5d);
    final Solution compromise = solution(2, 8d, 1d);
    final Solution third = solution(3, 6d, 3d);
    final List<Solution> front = List.of(first, compromise, third);

    Assertions.assertSame(compromise, FrontSolutionRanker.selectBestAverageRank(front, 0));
    Assertions.assertEquals(1.5d, FrontSolutionRanker.averageRank(compromise, front), 1e-12);
  }

  @Test
  void selectsHighestScoreForOneMaximizeObjective() {
    final Solution lower = solution(1, 5d);
    final Solution higher = solution(2, 7d);

    Assertions.assertSame(higher,
        FrontSolutionRanker.selectBestAverageRank(List.of(lower, higher), 0));
  }

  private static @NotNull Solution solution(int evaluation, double @NotNull ... values) {
    final Solution solution = new Solution(0, values.length);
    for (int i = 0; i < values.length; i++) {
      solution.setObjective(i, i == 1 ? new Minimize("Minimize") : new Maximize("Maximize"));
      solution.setObjectiveValue(i, values[i]);
    }
    solution.setAttribute(WizardOptimizationProblem.ATTR_PROPOSAL_INDEX, evaluation);
    return solution;
  }
}
