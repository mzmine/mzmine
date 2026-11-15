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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.moeaframework.algorithm.AbstractAlgorithm;
import org.moeaframework.algorithm.MOEAD;
import org.moeaframework.core.Solution;
import org.moeaframework.problem.AbstractProblem;

public class MoeadOptimizerModule implements OptimizerAlgorithmModule {

  private static final Logger logger = Logger.getLogger(MoeadOptimizerModule.class.getName());

  /**
   * Initial population size for MOEA/D. One evaluation is a full batch run, so the MOEA Framework
   * default of 100 would spend the entire batch budget on initialization and leave no generations
   * for the actual search.
   */
  private static final int POPULATION_SIZE = 20;

  @Override
  public @NotNull String getName() {
    return "MOEA/D";
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return MoeadOptimizerParameters.class;
  }

  @Override
  public @NotNull List<SweepMetric> getOptimizationTargets(@NotNull ParameterSet parameters) {
    return parameters.getValue(MoeadOptimizerParameters.optimizationTargets);
  }

  @Override
  public void setOptimizationTargets(@NotNull ParameterSet parameters,
      @NotNull List<SweepMetric> targets) {
    parameters.setParameter(MoeadOptimizerParameters.optimizationTargets, new ArrayList<>(targets));
  }

  @Override
  public @NotNull AbstractAlgorithm createAlgorithm(@NotNull AbstractProblem problem,
      @NotNull ParameterSet parameters) {
    final MOEAD moead = new MOEAD(problem);
    moead.setInitialPopulationSize(POPULATION_SIZE);
    // empty without raw data initialization, MOEA/D then initializes randomly
    final List<Solution> injected = createStartSolutions(problem, parameters);
    moead.setInitialization(new OriginTaggingInitialization(problem, injected));
    // at the MOEA/D default of 20 the neighborhood equals the population, so mating draws
    // from the whole population and the decomposition loses the locality it relies on.
    // assumption: the floor of 4 keeps the neighborhood above the differential evolution arity
    // of 4 minus 1, which MOEA/D requires now that the solution vector is all real-valued
    moead.setNeighborhoodSize(Math.max(4, POPULATION_SIZE / 5));
    // the variation operator is chosen in the MOEAD constructor from whether every variable is
    // real-valued, so log it - "de+pm" confirms MOEA/D-DE, "sbx+pm+hux+bf" means the vector
    // still contains a non-real variable and the decomposition fell back to SBX
    logger.info(
        "MOEA/D using variation %s, neighborhood %d, population %d, %d raw data based solutions".formatted(
            moead.getVariation().getName(), moead.getNeighborhoodSize(), POPULATION_SIZE,
            injected.size()));
    return moead;
  }

  private static @NotNull List<Solution> createStartSolutions(@NotNull AbstractProblem problem,
      @NotNull ParameterSet parameters) {
    if (!parameters.getValue(MoeadOptimizerParameters.rawDataInitialization)) {
      return List.of();
    }
    final WarmStartSampling sampling = parameters.getEmbeddedParameterValue(
        MoeadOptimizerParameters.rawDataInitialization);
    return WarmStartInitialization.createSolutions(problem, POPULATION_SIZE, sampling);
  }
}
