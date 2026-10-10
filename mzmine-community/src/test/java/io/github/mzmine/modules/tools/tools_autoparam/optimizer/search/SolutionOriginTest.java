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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.operator.real.SBX;

/**
 * Pins the contract the results table relies on to tell the phases of an optimization run apart:
 * warm-start solutions carry the origin they were built with, the random fill of the initial
 * population is labelled by {@link OriginTaggingInitialization}, and offspring arrive untagged so
 * they can be recognised as evolution.
 * <p>
 * The last one depends on MOEA Framework's {@link Solution#copy()} not copying attributes. If that
 * ever changes, every child inherits its parent's origin and the whole run reads as warm start
 * without anything failing, which is why it is asserted here.
 */
public class SolutionOriginTest {

  private static final int POPULATION_SIZE = 5;

  private TwoRealProblem problem;

  @BeforeEach
  void initialize() {
    PRNG.setSeed(42);
    problem = new TwoRealProblem();
  }

  @Test
  @DisplayName("an existing origin is never overwritten")
  void applyIfAbsentKeepsTheExistingOrigin() {
    final Solution solution = problem.newSolution();
    SolutionOrigin.PERTURBED.applyTo(solution);

    SolutionOrigin.EVOLUTION.applyIfAbsent(solution);

    Assertions.assertEquals(SolutionOrigin.PERTURBED, SolutionOrigin.of(solution));
  }

  @Test
  @DisplayName("only the random fill of the initial population is tagged random")
  void injectedSolutionsKeepTheirOrigin() {
    final Solution estimate = problem.newSolution();
    SolutionOrigin.ESTIMATE.applyTo(estimate);
    final Solution perturbed = problem.newSolution();
    SolutionOrigin.PERTURBED.applyTo(perturbed);

    final Solution[] population = new OriginTaggingInitialization(problem,
        List.of(estimate, perturbed)).initialize(POPULATION_SIZE);

    Assertions.assertEquals(POPULATION_SIZE, population.length);
    Assertions.assertEquals(SolutionOrigin.ESTIMATE, SolutionOrigin.of(population[0]));
    Assertions.assertEquals(SolutionOrigin.PERTURBED, SolutionOrigin.of(population[1]));
    for (int i = 2; i < population.length; i++) {
      Assertions.assertEquals(SolutionOrigin.RANDOM, SolutionOrigin.of(population[i]),
          "slot %d was filled randomly and has to say so".formatted(i));
    }
  }

  @Test
  @DisplayName("a population without injected solutions is entirely random")
  void anEmptyInjectionYieldsAllRandom() {
    final Solution[] population = new OriginTaggingInitialization(problem, List.of()).initialize(
        POPULATION_SIZE);

    for (final Solution solution : population) {
      Assertions.assertEquals(SolutionOrigin.RANDOM, SolutionOrigin.of(solution));
    }
  }

  @Test
  @DisplayName("offspring do not inherit the origin of their parents")
  void offspringAreCountedAsEvolution() {
    final Solution[] parents = new OriginTaggingInitialization(problem, List.of()).initialize(2);
    SolutionOrigin.PERTURBED.applyTo(parents[0]);
    SolutionOrigin.ESTIMATE.applyTo(parents[1]);

    // probability 1 so the crossover definitely fires
    final Solution[] offspring = new SBX(1.0, 15.0).evolve(parents);

    Assertions.assertTrue(offspring.length > 0, "the operator produced no offspring");
    for (final Solution child : offspring) {
      Assertions.assertNull(SolutionOrigin.of(child),
          "an offspring must reach the evaluation untagged, otherwise it is mistaken for a guess");
      SolutionOrigin.EVOLUTION.applyIfAbsent(child);
      Assertions.assertEquals(SolutionOrigin.EVOLUTION, SolutionOrigin.of(child));
    }
  }

  @Test
  @DisplayName("the label is what the table and the csv render")
  void toStringYieldsTheLabel() {
    Assertions.assertEquals("Estimate", SolutionOrigin.ESTIMATE.toString());
    Assertions.assertEquals("Perturbed", SolutionOrigin.PERTURBED.toString());
    Assertions.assertEquals("Random", SolutionOrigin.RANDOM.toString());
    Assertions.assertEquals("Evolution", SolutionOrigin.EVOLUTION.toString());
    Assertions.assertEquals("Pattern search", SolutionOrigin.PATTERN_SEARCH.toString());
  }
}
