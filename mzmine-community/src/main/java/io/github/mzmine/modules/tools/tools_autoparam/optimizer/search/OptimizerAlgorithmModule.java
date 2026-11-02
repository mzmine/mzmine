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

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.metrics.SweepMetric;
import io.github.mzmine.parameters.ParameterSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.moeaframework.algorithm.AbstractAlgorithm;
import org.moeaframework.problem.AbstractProblem;

/**
 * A search algorithm selectable in {@link OptimizerOptions}. The module owns everything specific to
 * its algorithm: the optimization targets in its parameters, the algorithm configuration, and the
 * start solutions around the raw data estimate. A new algorithm needs an implementation of this
 * interface, its parameter set, and an {@link OptimizerOptions} entry.
 */
public interface OptimizerAlgorithmModule extends MZmineModule {

  /**
   * @param parameters this module's parameters
   * @return the quality metrics optimized as objectives
   */
  @NotNull List<SweepMetric> getOptimizationTargets(@NotNull ParameterSet parameters);

  /**
   * @param parameters this module's parameters, modified
   * @param targets    the quality metrics to optimize as objectives
   * @throws IllegalArgumentException if the algorithm does not support the number of targets
   */
  void setOptimizationTargets(@NotNull ParameterSet parameters, @NotNull List<SweepMetric> targets);

  /**
   * Creates and configures the algorithm, including its start solutions.
   *
   * @param problem    the problem with the raw data estimate as initial values
   * @param parameters this module's parameters
   */
  @NotNull AbstractAlgorithm createAlgorithm(@NotNull AbstractProblem problem,
      @NotNull ParameterSet parameters);
}
