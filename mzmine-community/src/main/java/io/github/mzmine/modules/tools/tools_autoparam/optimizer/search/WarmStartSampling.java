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

import org.apache.commons.math3.distribution.NormalDistribution;
import org.jetbrains.annotations.NotNull;
import org.moeaframework.core.PRNG;
import org.moeaframework.util.sequence.LatinHypercube;
import org.moeaframework.util.sequence.Sequence;
import org.moeaframework.util.sequence.Sobol;

/**
 * How the warm-start perturbations are spread around the raw data estimate.
 * <p>
 * The warm start decides the result: across seven reference datasets it reached 86 to 99 % of a
 * run's final score, and the evolution that follows adds a median of 1.5 %. It is also where the
 * whole seed dependence comes from - the same data and settings score 1.19 to 1.93 times
 * differently depending only on the seed, and that spread is the same before and after the
 * evolution phase.
 * <p>
 * Independent draws leave the coverage of that neighbourhood to chance. A space-filling sequence
 * guarantees it by construction, which is the point of offering these alternatives.
 * <p>
 * decision: all three stratify the <em>perturbation</em>, not the parameter box. A design spread
 * over the whole box was measured 55 % worse than the estimate it replaced, so spreading uniformly
 * over the box more evenly would only make a bad starting set more reliably bad.
 */
public enum WarmStartSampling {

  /**
   * One independent Gaussian per variable per solution. The original behaviour.
   */
  GAUSSIAN("Independent draws"),

  /**
   * Latin hypercube: each variable's range of deviations is divided into as many strata as there
   * are solutions and every stratum is used exactly once, so no run can miss a part of the
   * neighbourhood.
   */
  LATIN_HYPERCUBE("Latin hypercube"),

  /**
   * A Sobol low-discrepancy sequence, which spreads evenly in every projection and, unlike the
   * other two, does not consult the random generator at all.
   */
  SOBOL("Sobol sequence");

  /**
   * Keeps the mapped deviate finite when a sequence returns exactly 0 or 1, which Sobol does for
   * its first point. Clamping at 1e-4 caps a deviate at about 3.7 standard deviations.
   */
  private static final double TAIL_LIMIT = 1e-4;

  private static final NormalDistribution STANDARD_NORMAL = new NormalDistribution(0, 1);

  private final @NotNull String label;

  WarmStartSampling(@NotNull String label) {
    this.label = label;
  }

  /**
   * Standard normal deviates to perturb with, one row per solution and one column per variable.
   * <p>
   * assumption: every variable consumes exactly one deviate, which holds because the problem builds
   * its solutions from {@link org.moeaframework.core.variable.RealVariable}s throughout. A variable
   * that skipped its deviate would shift the whole row for the sampled sequences.
   *
   * @param count      solutions to perturb
   * @param dimensions variables per solution
   */
  public @NotNull double[][] normalDeviates(int count, int dimensions) {
    if (count <= 0 || dimensions <= 0) {
      return new double[0][0];
    }
    if (this == GAUSSIAN) {
      final double[][] deviates = new double[count][dimensions];
      for (int i = 0; i < count; i++) {
        for (int j = 0; j < dimensions; j++) {
          deviates[i][j] = PRNG.nextGaussian();
        }
      }
      return deviates;
    }

    final Sequence sequence = this == SOBOL ? new Sobol() : new LatinHypercube();
    final double[][] uniforms = sequence.generate(count, dimensions);
    final double[][] deviates = new double[count][dimensions];
    for (int i = 0; i < count; i++) {
      for (int j = 0; j < dimensions; j++) {
        final double u = Math.clamp(uniforms[i][j], TAIL_LIMIT, 1 - TAIL_LIMIT);
        deviates[i][j] = STANDARD_NORMAL.inverseCumulativeProbability(u);
      }
    }
    return deviates;
  }

  @Override
  public String toString() {
    return label;
  }
}
