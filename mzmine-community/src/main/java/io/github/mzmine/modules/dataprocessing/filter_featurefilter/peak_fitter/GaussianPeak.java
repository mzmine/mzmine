/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter;

import java.util.List;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.ParameterValidator;
import org.jetbrains.annotations.Nullable;


/**
 * Models an ideal peak using a gaussian.
 */
public class GaussianPeak implements PeakModelFunction {

  public GaussianPeak() {
    super();
  }

  /**
   * Parameters: [0] = Amplitude (A), [1] = Mean (mu), [2] = Sigma (std dev)
   */
  @Override
  public double value(double x, double... parameters) {
    if (parameters == null) {
      throw new NullArgumentException();
    }
    if (parameters.length != 3) {
      throw new DimensionMismatchException(parameters.length, 3);
    }
    double a = parameters[0];
    double mu = parameters[1];
    double sigma = parameters[2];
    if (sigma == 0) {
      return (x == mu) ? a : 0; // Avoid division by zero, delta function
    }
    return a * Math.exp(-Math.pow(x - mu, 2) / (2 * sigma * sigma));
  }

  /**
   * Gradient: partial derivatives with respect to A, mu, sigma
   * dF/dA, dF/dmu, dF/dsigma
   * This is needed by LevenbergMarquardtOptimizer for efficiency.
   */
  @Override
  public double[] gradient(double x, double... parameters) {
    if (parameters == null) {
      throw new NullArgumentException();
    }
    if (parameters.length != 3) {
      throw new DimensionMismatchException(parameters.length, 3);
    }
    double a = parameters[0];
    double mu = parameters[1];
    double sigma = parameters[2];

    double[] grad = new double[3];
    double expTerm = Math.exp(-Math.pow(x - mu, 2) / (2 * sigma * sigma));
    double commonFactor = Math.pow(x - mu, 2);

    grad[0] = expTerm; // dF/dA
    if (sigma == 0) {
      grad[1] = 0; // Undefined derivative at sigma=0, but LM should avoid this
      grad[2] = 0;
    } else {
      grad[1] = a * expTerm * (x - mu) / (sigma * sigma); // dF/dmu
      grad[2] = a * expTerm * commonFactor / (Math.pow(sigma, 3)); // dF/dsigma
    }
    return grad;
  }

  @Override
  public @Nullable ParameterValidator getValidator() {
    return null;
  }

  @Override
  public double @Nullable [] guessStartParameters(List<WeightedObservedPoint> points) {
    return InitialGuess.guessGaussian(points);
  }

  @Override
  public PeakShapeClassification getPeakType() {
    return PeakShapeClassification.GAUSSIAN;
  }
}
