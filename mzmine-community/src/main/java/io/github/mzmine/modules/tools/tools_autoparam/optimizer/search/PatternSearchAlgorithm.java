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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.moeaframework.algorithm.AbstractAlgorithm;
import org.moeaframework.core.Solution;
import org.moeaframework.core.comparator.DominanceComparator;
import org.moeaframework.core.comparator.ParetoDominanceComparator;
import org.moeaframework.core.population.NondominatedPopulation;
import org.moeaframework.problem.Problem;
import org.moeaframework.util.sequence.Sobol;

/**
 * Deterministic coordinate pattern search in the effective, normalized parameter space.
 */
public final class PatternSearchAlgorithm extends AbstractAlgorithm {

  public static final int INITIAL_DESIGN_SIZE = 1;

  private static final double INITIAL_STEP = 0.20d;
  private static final double MINIMUM_CONTINUOUS_STEP = 0.01d;
  private static final double RESTART_RADIUS = 0.25d;
  private static final int MAXIMUM_RESTART_CANDIDATES = 4_096;

  private final @NotNull CanonicalParameterSpace parameterSpace;
  private final @NotNull DominanceComparator comparator = new ParetoDominanceComparator();
  private final @NotNull List<Solution> observations = new ArrayList<>();
  private final @NotNull Set<List<Double>> visited = new LinkedHashSet<>();
  private final @NotNull Deque<double[]> pendingDirections = new ArrayDeque<>();
  private final double[] steps;
  private final double[][] restartDesign;

  private @NotNull List<Solution> initialSolutions = List.of();
  private @Nullable Solution incumbent;
  private @Nullable Solution coordinateBest;
  private int sweepStart;
  private int coordinatesVisited;
  private int activeCoordinate = -1;
  private int restartIndex;
  private boolean sweepImproved;
  private boolean evaluatingRestart;

  public PatternSearchAlgorithm(@NotNull Problem problem) {
    super(problem);
    if (problem.getNumberOfObjectives() != 1) {
      throw new IllegalArgumentException("Pattern search supports exactly one objective");
    }
    if (problem.getNumberOfVariables() == 0) {
      throw new IllegalArgumentException("Pattern search requires at least one variable");
    }
    parameterSpace = new CanonicalParameterSpace(problem);
    steps = new double[parameterSpace.dimensions()];
    resetSteps();
    restartDesign = new Sobol().generate(MAXIMUM_RESTART_CANDIDATES, parameterSpace.dimensions());
  }

  /**
   * Supplies the explicit warm start. An empty list starts deterministically at the box center.
   */
  public void setInitialSolutions(@NotNull List<Solution> initialSolutions) {
    assertNotInitialized();
    this.initialSolutions = List.copyOf(initialSolutions);
  }

  @Override
  public void initialize() {
    super.initialize();

    if (initialSolutions.isEmpty()) {
      final double[] center = new double[parameterSpace.dimensions()];
      Arrays.fill(center, 0.5d);
      final Solution initial = parameterSpace.materialize(center, SolutionOrigin.PATTERN_SEARCH);
      evaluateAndObserve(initial);
      incumbent = initial;
      return;
    }

    for (final Solution initial : initialSolutions) {
      final List<Double> key = parameterSpace.key(initial);
      if (visited.add(key)) {
        evaluateAndObserve(initial);
        if (isBetter(initial, incumbent)) {
          incumbent = initial;
        }
      }
    }
  }

  @Override
  protected void iterate() {
    final double[] canonical = nextCandidate();
    if (canonical == null) {
      terminate();
      return;
    }

    final Solution candidate = parameterSpace.materialize(canonical, SolutionOrigin.PATTERN_SEARCH);
    evaluateAndObserve(candidate);

    if (evaluatingRestart) {
      if (isBetter(candidate, incumbent)) {
        incumbent = candidate;
        resetSteps();
      }
      resetSweep();
    } else {
      if (isBetter(candidate, coordinateBest)) {
        coordinateBest = candidate;
      }
      if (pendingDirections.isEmpty()) {
        finishCoordinate();
      }
    }
  }

  @Override
  public @NotNull NondominatedPopulation getResult() {
    final NondominatedPopulation result = new NondominatedPopulation();
    if (incumbent != null) {
      result.add(incumbent);
    }
    return result;
  }

  @Override
  public @NotNull String getName() {
    return "Pattern Search";
  }

  @NotNull List<Solution> getObservations() {
    return List.copyOf(observations);
  }

  private void evaluateAndObserve(@NotNull Solution solution) {
    evaluate(solution);
    observations.add(solution);
    visited.add(parameterSpace.key(solution));
  }

  private @Nullable double[] nextCandidate() {
    evaluatingRestart = false;
    while (pendingDirections.isEmpty()) {
      if (coordinatesVisited < parameterSpace.dimensions()) {
        prepareCoordinate();
        continue;
      }

      if (!sweepImproved && allStepsAtMinimum()) {
        evaluatingRestart = true;
        return nextRestartCandidate();
      }
      resetSweep();
    }
    return pendingDirections.removeFirst();
  }

  private void prepareCoordinate() {
    final Solution current = incumbent;
    if (current == null) {
      return;
    }
    final int dimension = (sweepStart + coordinatesVisited) % parameterSpace.dimensions();
    coordinatesVisited++;
    activeCoordinate = dimension;
    final double[] center = parameterSpace.encode(current);
    addDirection(center, dimension, steps[dimension]);
    addDirection(center, dimension, -steps[dimension]);
    if (pendingDirections.isEmpty()) {
      reduceStep(dimension);
      activeCoordinate = -1;
      return;
    }
    coordinateBest = current;
  }

  private void addDirection(@NotNull double[] center, int dimension, double offset) {
    final double[] candidate = center.clone();
    candidate[dimension] = Math.clamp(candidate[dimension] + offset, 0d, 1d);
    final double[] effective = parameterSpace.canonicalize(candidate);
    if (!Arrays.equals(center, effective) && visited.add(parameterSpace.key(effective))) {
      pendingDirections.addLast(effective);
    }
  }

  private void finishCoordinate() {
    if (isBetter(coordinateBest, incumbent)) {
      incumbent = coordinateBest;
      sweepImproved = true;
    } else if (activeCoordinate >= 0) {
      reduceStep(activeCoordinate);
    }
    activeCoordinate = -1;
    coordinateBest = null;
  }

  private boolean allStepsAtMinimum() {
    for (int i = 0; i < steps.length; i++) {
      if (steps[i] > minimumStep(i)) {
        return false;
      }
    }
    return true;
  }

  private void reduceStep(int dimension) {
    steps[dimension] = Math.max(minimumStep(dimension), steps[dimension] / 2d);
  }

  private void resetSweep() {
    sweepStart = (sweepStart + 1) % parameterSpace.dimensions();
    coordinatesVisited = 0;
    sweepImproved = false;
    coordinateBest = null;
    activeCoordinate = -1;
    pendingDirections.clear();
  }

  private void resetSteps() {
    for (int i = 0; i < steps.length; i++) {
      steps[i] = Math.max(INITIAL_STEP, minimumStep(i));
    }
  }

  private double minimumStep(int dimension) {
    return parameterSpace.isOrdinal(dimension) ? parameterSpace.ordinalStep(dimension)
        : MINIMUM_CONTINUOUS_STEP;
  }

  private @Nullable double[] nextRestartCandidate() {
    final Solution current = incumbent;
    if (current == null) {
      return null;
    }
    final double[] center = parameterSpace.encode(current);
    while (restartIndex < restartDesign.length) {
      final double[] uniform = restartDesign[restartIndex++];
      final double[] candidate = new double[parameterSpace.dimensions()];
      for (int i = 0; i < candidate.length; i++) {
        candidate[i] = Math.clamp(center[i] + (2d * uniform[i] - 1d) * RESTART_RADIUS, 0d, 1d);
      }
      final double[] effective = parameterSpace.canonicalize(candidate);
      if (visited.add(parameterSpace.key(effective))) {
        return effective;
      }
    }
    return null;
  }

  private boolean isBetter(@Nullable Solution candidate, @Nullable Solution reference) {
    return candidate != null && (reference == null || comparator.compare(candidate, reference) < 0);
  }
}
