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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.local_max;

import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DiagonalMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Levenberg-Marquardt fit of a peak model with per-point weights, numeric derivatives and a
 * parameter validator for bounds. Used by the {@link EmgFitter}.
 */
public final class WeightedLeastSquares {

  private static final int MAX_ITERATIONS = 200;
  private static final int MAX_EVALUATIONS = 2000;

  private WeightedLeastSquares() {
  }

  /**
   * @param y         observed values
   * @param weights   weight per observation
   * @param model     maps parameters to the model value of every observation
   * @param start     start parameters
   * @param validator clamps parameters into their bounds after each step
   * @return the optimized parameters or null if the optimizer failed
   */
  public static double @Nullable [] fit(final double @NotNull [] y,
      final double @NotNull [] weights, @NotNull final Function<double[], double[]> model,
      final double @NotNull [] start, @NotNull final UnaryOperator<double[]> validator) {
    final int n = y.length;
    final MultivariateJacobianFunction jacobianModel = point -> {
      final double[] p = point.toArray();
      final double[] values = model.apply(p);
      final double[][] jacobian = new double[n][p.length];
      // numeric forward differences, one model evaluation per parameter. The step is larger than
      // for central differences to balance truncation and rounding errors
      for (int j = 0; j < p.length; j++) {
        final double h = 1e-5 * Math.max(1, Math.abs(p[j]));
        final double[] plus = p.clone();
        plus[j] += h;
        final double[] vPlus = model.apply(plus);
        for (int i = 0; i < n; i++) {
          jacobian[i][j] = (vPlus[i] - values[i]) / h;
        }
      }
      return new Pair<RealVector, RealMatrix>(new ArrayRealVector(values, false),
          new Array2DRowRealMatrix(jacobian, false));
    };

    final LeastSquaresProblem problem = new LeastSquaresBuilder().start(start).model(jacobianModel)
        .target(y).weight(new DiagonalMatrix(weights.clone()))
        .parameterValidator(params -> new ArrayRealVector(validator.apply(params.toArray()), false))
        .maxIterations(MAX_ITERATIONS).maxEvaluations(MAX_EVALUATIONS).lazyEvaluation(false)
        .build();
    try {
      final Optimum optimum = new LevenbergMarquardtOptimizer().optimize(problem);
      return optimum.getPoint().toArray();
    } catch (RuntimeException e) {
      return null;
    }
  }
}
