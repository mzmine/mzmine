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

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moeaframework.core.Solution;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.core.variable.RealVariable;

class PatternSearchAlgorithmTest {

  private static @NotNull PatternSearchAlgorithm initializedAt(@NotNull QuadraticProblem problem,
      double... values) {
    final Solution initial = problem.newSolution();
    for (int i = 0; i < values.length; i++) {
      RealVariable.setReal(initial.getVariable(i), values[i]);
    }
    SolutionOrigin.ESTIMATE.applyTo(initial);
    final PatternSearchAlgorithm algorithm = new PatternSearchAlgorithm(problem);
    algorithm.setInitialSolutions(List.of(initial));
    return algorithm;
  }

  @Test
  void reachesAMonotoneBoundaryOptimum() {
    final QuadraticProblem problem = new QuadraticProblem(1d);
    final PatternSearchAlgorithm algorithm = initializedAt(problem, 0.2d);

    algorithm.run(12);

    final NondominatedPopulation result = algorithm.getResult();
    Assertions.assertEquals(1, result.size());
    Assertions.assertEquals(1d, RealVariable.getReal(result.get(0).getVariable(0)), 1e-12);
    Assertions.assertEquals(12, algorithm.getNumberOfEvaluations());
  }

  @Test
  void contractsTheStepAroundAnInteriorOptimum() {
    final QuadraticProblem problem = new QuadraticProblem(0.65d);
    final PatternSearchAlgorithm algorithm = initializedAt(problem, 0.2d);

    algorithm.run(20);

    final double best = RealVariable.getReal(algorithm.getResult().get(0).getVariable(0));
    Assertions.assertEquals(0.65d, best, 0.051d);
  }

  @Test
  void contractsOnlyFailedCoordinatesAndKeepsSuccessfulSteps() {
    final QuadraticProblem problem = new QuadraticProblem(Double.NaN, 0.8d);
    final PatternSearchAlgorithm algorithm = initializedAt(problem, 0.5d, 0.2d);

    algorithm.run(8);

    final List<List<Double>> evaluations = problem.evaluations();
    // The second coordinate improved from 0.2 to 0.4, so its next move remains 0.2.
    Assertions.assertEquals(0.6d, evaluations.get(5).get(1), 1e-12);
    // The flat first coordinate failed in both directions, so its next move halves to 0.1.
    Assertions.assertEquals(0.6d, evaluations.get(6).get(0), 1e-12);
    Assertions.assertEquals(0.4d, evaluations.get(7).get(0), 1e-12);
  }

  @Test
  void proposalSequenceIsDeterministicAndCandidatesAreTagged() {
    final QuadraticProblem firstProblem = new QuadraticProblem(0.7d, Double.NaN);
    final QuadraticProblem secondProblem = new QuadraticProblem(0.7d, Double.NaN);
    final PatternSearchAlgorithm first = initializedAt(firstProblem, 0.2d, 0.8d);
    final PatternSearchAlgorithm second = initializedAt(secondProblem, 0.2d, 0.8d);

    first.run(24);
    second.run(24);

    Assertions.assertEquals(firstProblem.evaluations(), secondProblem.evaluations());
    Assertions.assertTrue(first.getObservations().stream().skip(1)
        .allMatch(s -> SolutionOrigin.of(s) == SolutionOrigin.PATTERN_SEARCH));
    Assertions.assertEquals(first.getNumberOfEvaluations(), firstProblem.evaluations().size());
  }

  @Test
  void emptyWarmStartUsesTheCanonicalCenter() {
    final QuadraticProblem problem = new QuadraticProblem(0.5d, 0.5d);
    final PatternSearchAlgorithm algorithm = new PatternSearchAlgorithm(problem);

    algorithm.step();

    Assertions.assertEquals(List.of(0.5d, 0.5d), problem.evaluations().getFirst());
    Assertions.assertEquals(SolutionOrigin.PATTERN_SEARCH,
        SolutionOrigin.of(algorithm.getObservations().getFirst()));
  }

  @Test
  void constraintDominanceKeepsTheIncumbentFeasible() {
    final ConstrainedQuadraticProblem problem = new ConstrainedQuadraticProblem();
    final Solution initial = problem.newSolution();
    RealVariable.setReal(initial.getVariable(0), 0.2d);
    final PatternSearchAlgorithm algorithm = new PatternSearchAlgorithm(problem);
    algorithm.setInitialSolutions(List.of(initial));

    algorithm.run(16);

    final Solution result = algorithm.getResult().get(0);
    Assertions.assertTrue(result.isFeasible());
    Assertions.assertEquals(0.6d, RealVariable.getReal(result.getVariable(0)), 0.011d);
  }

  @Test
  void ordinalStepsReachEachEffectiveLevelWithoutDuplicates() {
    final OrdinalQuadraticProblem problem = new OrdinalQuadraticProblem();
    final Solution initial = problem.newSolution();
    RealVariable.setReal(initial.getVariable(0), 2d);
    final PatternSearchAlgorithm algorithm = new PatternSearchAlgorithm(problem);
    algorithm.setInitialSolutions(List.of(initial));

    algorithm.run(10);

    Assertions.assertEquals(5, OrdinalIntegerVariable.getInt(algorithm.getResult().get(0), 0));
    final long distinct = algorithm.getObservations().stream()
        .map(s -> OrdinalIntegerVariable.getInt(s, 0)).distinct().count();
    Assertions.assertEquals(algorithm.getNumberOfEvaluations(), distinct);
  }

  @Test
  void exhaustedMinimumStepFallsBackToAnUnusedSobolPerturbation() {
    final QuadraticProblem problem = new QuadraticProblem(Double.NaN);
    final PatternSearchAlgorithm algorithm = initializedAt(problem, 0.5d);

    algorithm.run(20);

    Assertions.assertEquals(20, algorithm.getNumberOfEvaluations());
    Assertions.assertTrue(problem.evaluations().stream()
        .anyMatch(values -> Math.abs(values.getFirst() - 0.25d) < 1e-12));
    Assertions.assertEquals(20, problem.evaluations().stream().distinct().count());
  }
}
