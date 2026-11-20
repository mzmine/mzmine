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

import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.SweepMetric;
import io.github.mzmine.parameters.ParameterSet;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.moeaframework.algorithm.AbstractAlgorithm;
import org.moeaframework.core.Solution;
import org.moeaframework.problem.AbstractProblem;

public class PatternSearchOptimizerModule implements OptimizerAlgorithmModule {

  private static final Logger logger = Logger.getLogger(
      PatternSearchOptimizerModule.class.getName());

  @Override
  public @NotNull String getName() {
    return "Pattern search";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return PatternSearchOptimizerParameters.class;
  }

  @Override
  public @NotNull List<SweepMetric> getOptimizationTargets(@NotNull ParameterSet parameters) {
    return List.of(parameters.getValue(PatternSearchOptimizerParameters.optimizationTarget));
  }

  @Override
  public void setOptimizationTargets(@NotNull ParameterSet parameters,
      @NotNull List<SweepMetric> targets) {
    if (targets.size() != 1) {
      throw new IllegalArgumentException("Pattern search requires exactly one target.");
    }
    parameters.setParameter(PatternSearchOptimizerParameters.optimizationTarget,
        targets.getFirst());
  }

  @Override
  public @NotNull AbstractAlgorithm createAlgorithm(@NotNull AbstractProblem problem,
      @NotNull ParameterSet parameters) {
    final PatternSearchAlgorithm algorithm = new PatternSearchAlgorithm(problem);
    // decision: starting at the estimate is intrinsic to local pattern search, not an optional
    // warm-start strategy.
    final List<Solution> initial = WarmStartInitialization.createSolutions(problem,
        PatternSearchAlgorithm.INITIAL_DESIGN_SIZE, WarmStartSampling.GAUSSIAN);
    algorithm.setInitialSolutions(initial);
    logger.info(
        "Pattern search starts from %d raw data based solution(s)".formatted(initial.size()));
    return algorithm;
  }
}
