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
import org.jetbrains.annotations.Nullable;
import org.moeaframework.core.PRNG;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.RealVariable;
import org.moeaframework.problem.AbstractProblem;

/**
 * Samples around the initialized parameter vector without any parameter-name lookup.
 */
public final class WarmStartInitialization {

  private static final double WARM_START_PERTURBATION = 0.20;
  private static final double LOG_WARM_START_PERTURBATION = 0.5;

  private WarmStartInitialization() {
  }

  public static @NotNull List<Solution> createSolutions(@NotNull AbstractProblem problem, int count,
      @NotNull WarmStartSampling sampling) {
    final List<Solution> solutions = new ArrayList<>(count);
    final double[][] deviates = sampling.normalDeviates(Math.max(0, count - 1),
        problem.getNumberOfVariables());
    final SearchScaleProvider scales =
        problem instanceof SearchScaleProvider provider ? provider : _ -> SearchScale.LINEAR;
    for (int i = 0; i < count; i++) {
      final Solution solution = problem.newSolution();
      if (i == 0) {
        SolutionOrigin.ESTIMATE.applyTo(solution);
      } else {
        perturb(solution, WARM_START_PERTURBATION, deviates[i - 1], scales);
        SolutionOrigin.PERTURBED.applyTo(solution);
      }
      solutions.add(solution);
    }
    return List.copyOf(solutions);
  }

  public static void perturb(@NotNull Solution solution, double perturbationFraction,
      @NotNull SearchScaleProvider scales) {
    perturb(solution, perturbationFraction, null, scales);
  }

  private static void perturb(@NotNull Solution solution, double perturbationFraction,
      double @Nullable [] normalDeviates, @NotNull SearchScaleProvider scales) {
    for (int i = 0; i < solution.getNumberOfVariables(); i++) {
      final RealVariable variable = (RealVariable) solution.getVariable(i);
      final double center = variable.getValue();
      if (!Double.isFinite(center)) {
        throw new IllegalArgumentException("Warm starts require initialized parameters");
      }
      final double noise = normalDeviates == null ? PRNG.nextGaussian() : normalDeviates[i];
      final boolean logarithmic = !(variable instanceof OrdinalIntegerVariable)
          && scales.searchScale(i) == SearchScale.LOGARITHMIC;
      if (logarithmic && variable.getLowerBound() <= 0d) {
        throw new IllegalArgumentException("Logarithmic search requires positive bounds");
      }
      final double value = logarithmic ? center * Math.exp(noise * LOG_WARM_START_PERTURBATION)
          : center + noise * perturbationFraction * (variable.getUpperBound()
                                                     - variable.getLowerBound());
      variable.setValue(Math.clamp(value, variable.getLowerBound(), variable.getUpperBound()));
    }
  }
}
