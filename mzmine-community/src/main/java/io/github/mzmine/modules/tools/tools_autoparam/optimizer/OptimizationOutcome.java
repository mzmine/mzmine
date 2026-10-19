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

import io.github.mzmine.modules.tools.tools_autoparam.estimation.PreparedParameterSet;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution.WizardOptimizationProblem;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.moeaframework.core.Solution;
import org.moeaframework.core.population.NondominatedPopulation;

/**
 * Everything a finished optimization produced, so a caller that is not the results window can read
 * it back. {@link BatchOptimizationMainTask} keeps these as locals otherwise, which makes the run
 * observable only through the GUI.
 *
 * @param estimates        the prepared parameter baseline, before any optimization
 * @param estimateSolution the evaluated estimate, so its scores can be compared against the
 *                         optimized solutions
 * @param front            non-dominated result across every completed observation
 * @param problem          the problem, which holds every evaluated solution in evaluation order
 */
public record OptimizationOutcome(@NotNull PreparedParameterSet estimates,
                                  @NotNull Solution estimateSolution,
                                  @NotNull NondominatedPopulation front,
                                  @NotNull WizardOptimizationProblem problem) {

  /**
   * Every solution that was evaluated, in evaluation order, including infeasible ones.
   */
  public @NotNull List<Solution> evaluatedSolutions() {
    return problem.getEvaluatedSolutions();
  }

}
