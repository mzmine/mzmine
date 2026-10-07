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
import org.jetbrains.annotations.NotNull;
import org.moeaframework.core.Solution;
import org.moeaframework.core.constraint.LessThanOrEqual;
import org.moeaframework.core.variable.RealVariable;
import org.moeaframework.problem.AbstractProblem;

final class MixedVariableProblem extends AbstractProblem implements SearchScaleProvider {

  MixedVariableProblem() {
    super(3, 1, 1);
  }

  @Override
  public void evaluate(@NotNull Solution solution) {
    solution.setObjectiveValue(0, OrdinalIntegerVariable.effectiveValue(solution, 0));
    solution.setConstraintValue(0, 0d);
  }

  @Override
  public @NotNull Solution newSolution() {
    final Solution solution = new Solution(3, 1, 1);
    solution.setVariable(0, new RealVariable("linear", 1d, 101d));
    solution.setVariable(1, new RealVariable("logarithmic", 1d, 9d));
    solution.setVariable(2, new OrdinalIntegerVariable("ordinal", 1, 4));
    solution.setConstraint(0, new LessThanOrEqual("constraint", 0d));
    return solution;
  }

  @Override
  public @NotNull SearchScale searchScale(int parameterIndex) {
    return switch (parameterIndex) {
      case 1 -> SearchScale.LOGARITHMIC;
      case 0, 2 -> SearchScale.LINEAR;
      default -> throw new IllegalArgumentException("Unknown parameter " + parameterIndex);
    };
  }
}
