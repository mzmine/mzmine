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
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.RealVariable;
import org.moeaframework.core.variable.Variable;
import org.moeaframework.problem.Problem;

/**
 * Maps the problem's effective parameter values to and from a unit hypercube.
 */
final class CanonicalParameterSpace {

  private final @NotNull Problem problem;
  private final double[] lowerBounds;
  private final double[] upperBounds;
  private final boolean[] logarithmic;
  private final boolean[] ordinal;
  private final int[] lowerIntegerBounds;
  private final int[] upperIntegerBounds;

  CanonicalParameterSpace(@NotNull Problem problem) {
    this.problem = problem;
    final Solution prototype = problem.newSolution();
    final int dimensions = prototype.getNumberOfVariables();
    lowerBounds = new double[dimensions];
    upperBounds = new double[dimensions];
    logarithmic = new boolean[dimensions];
    ordinal = new boolean[dimensions];
    lowerIntegerBounds = new int[dimensions];
    upperIntegerBounds = new int[dimensions];
    final SearchScaleProvider scaleProvider =
        problem instanceof SearchScaleProvider provider ? provider : _ -> SearchScale.LINEAR;

    for (int i = 0; i < dimensions; i++) {
      final Variable variable = prototype.getVariable(i);
      if (!(variable instanceof RealVariable real)) {
        throw new IllegalArgumentException(
            "Canonical parameter mapping requires real-valued variables, found "
                + variable.getClass());
      }
      if (real instanceof OrdinalIntegerVariable integer) {
        ordinal[i] = true;
        lowerIntegerBounds[i] = integer.getLowerIntBound();
        upperIntegerBounds[i] = integer.getUpperIntBound();
        lowerBounds[i] = lowerIntegerBounds[i];
        upperBounds[i] = upperIntegerBounds[i];
      } else {
        lowerBounds[i] = real.getLowerBound();
        upperBounds[i] = real.getUpperBound();
        logarithmic[i] = scaleProvider.searchScale(i) == SearchScale.LOGARITHMIC;
        if (logarithmic[i] && lowerBounds[i] <= 0d) {
          throw new IllegalArgumentException(
              "Logarithmic parameter %s requires a positive lower bound, found %s".formatted(
                  real.getName(), lowerBounds[i]));
        }
      }
    }
  }

  int dimensions() {
    return lowerBounds.length;
  }

  boolean isOrdinal(int dimension) {
    return ordinal[dimension];
  }

  double ordinalStep(int dimension) {
    final int range = upperIntegerBounds[dimension] - lowerIntegerBounds[dimension];
    return range > 0 ? 1d / range : 1d;
  }

  @NotNull double[] encode(@NotNull Solution solution) {
    if (solution.getNumberOfVariables() != dimensions()) {
      throw new IllegalArgumentException("solution has the wrong number of variables");
    }
    final double[] canonical = new double[dimensions()];
    for (int i = 0; i < dimensions(); i++) {
      final double value = Math.clamp(OrdinalIntegerVariable.effectiveValue(solution, i),
          lowerBounds[i], upperBounds[i]);
      if (upperBounds[i] == lowerBounds[i]) {
        canonical[i] = 0d;
      } else if (logarithmic[i]) {
        canonical[i] =
            (Math.log(value) - Math.log(lowerBounds[i])) / (Math.log(upperBounds[i]) - Math.log(
                lowerBounds[i]));
      } else {
        canonical[i] = (value - lowerBounds[i]) / (upperBounds[i] - lowerBounds[i]);
      }
    }
    return canonical;
  }

  @NotNull double[] canonicalize(@NotNull double[] canonical) {
    checkDimensions(canonical);
    final double[] effective = canonical.clone();
    for (int i = 0; i < dimensions(); i++) {
      final double clamped = Math.clamp(effective[i], 0d, 1d);
      if (ordinal[i]) {
        final int range = upperIntegerBounds[i] - lowerIntegerBounds[i];
        final int value = lowerIntegerBounds[i] + (int) Math.round(clamped * range);
        effective[i] = range == 0 ? 0d : (value - lowerIntegerBounds[i]) / (double) range;
      } else {
        effective[i] = clamped;
      }
    }
    return effective;
  }

  @NotNull List<Double> key(@NotNull double[] canonical) {
    final double[] effective = canonicalize(canonical);
    final List<Double> key = new ArrayList<>(effective.length);
    for (int i = 0; i < effective.length; i++) {
      key.add(Math.clamp(decode(effective[i], i), lowerBounds[i], upperBounds[i]));
    }
    return List.copyOf(key);
  }

  @NotNull List<Double> key(@NotNull Solution solution) {
    if (solution.getNumberOfVariables() != dimensions()) {
      throw new IllegalArgumentException("solution has the wrong number of variables");
    }
    final List<Double> key = new ArrayList<>(dimensions());
    for (int i = 0; i < dimensions(); i++) {
      final Variable variable = solution.getVariable(i);
      if (!(variable instanceof RealVariable real)) {
        throw new IllegalArgumentException(
            "Canonical parameter mapping requires real-valued variables, found "
                + variable.getClass());
      }
      key.add(
          real instanceof OrdinalIntegerVariable ? OrdinalIntegerVariable.effectiveValue(solution,
              i) : real.getValue());
    }
    return List.copyOf(key);
  }

  @NotNull Solution materialize(@NotNull double[] canonical, @NotNull SolutionOrigin origin) {
    final double[] effective = canonicalize(canonical);
    final Solution solution = problem.newSolution();
    for (int i = 0; i < dimensions(); i++) {
      final Variable variable = solution.getVariable(i);
      if (!(variable instanceof RealVariable real)) {
        throw new IllegalArgumentException(
            "Canonical parameter mapping requires real-valued variables, found "
                + variable.getClass());
      }
      final double value = decode(effective[i], i);
      real.setValue(Math.clamp(value, real.getLowerBound(), real.getUpperBound()));
    }
    origin.applyTo(solution);
    return solution;
  }

  private void checkDimensions(@NotNull double[] canonical) {
    if (canonical.length != dimensions()) {
      throw new IllegalArgumentException(
          "expected %d dimensions but found %d".formatted(dimensions(), canonical.length));
    }
  }

  private double decode(double canonical, int dimension) {
    if (ordinal[dimension]) {
      final int range = upperIntegerBounds[dimension] - lowerIntegerBounds[dimension];
      return lowerIntegerBounds[dimension] + Math.round(canonical * range);
    }
    if (logarithmic[dimension]) {
      return Math.exp(
          Math.log(lowerBounds[dimension]) + canonical * (Math.log(upperBounds[dimension])
              - Math.log(lowerBounds[dimension])));
    }
    return lowerBounds[dimension] + canonical * (upperBounds[dimension] - lowerBounds[dimension]);
  }
}
