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
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.ParameterValidator;
import org.jetbrains.annotations.Nullable;

/**
 * Used to model a fronting or tailing peak. Uses two gaussians internally. Both gaussians use the
 * same amplitude and center, but different sigma values.
 */
public class AsymmetricGaussianPeak implements PeakModelFunction {

  public AsymmetricGaussianPeak() {
    super();
  }

  /**
   * Parameters: [0]=Amplitude(A), [1]=Mean(mu), [2]=Sigma_left, [3]=Sigma_right
   */
  @Override
  public double value(double x, double... parameters) {
    if (parameters == null) {
      throw new NullArgumentException();
    }
    if (parameters.length != 4) {
      throw new DimensionMismatchException(parameters.length, 4);
    }
    double a = parameters[0];
    double mu = parameters[1];
    double sigmaLeft = parameters[2];
    double sigmaRight = parameters[3];

    if (x <= mu) {
      if (sigmaLeft == 0) {
        return (x == mu) ? a : 0;
      }
      return a * Math.exp(-Math.pow(x - mu, 2) / (2 * sigmaLeft * sigmaLeft));
    } else {
      if (sigmaRight == 0) {
        return (x == mu) ? a : 0; // Should not happen if x > mu
      }
      return a * Math.exp(-Math.pow(x - mu, 2) / (2 * sigmaRight * sigmaRight));
    }
  }

  /**
   * Gradient: partial derivatives with respect to A, mu, sigma_left, sigma_right dF/dA, dF/dmu,
   * dF/dsigma_left, dF/dsigma_right
   */
  @Override
  public double[] gradient(double x, double... parameters) {
    if (parameters == null) {
      throw new NullArgumentException();
    }
    if (parameters.length != 4) {
      throw new DimensionMismatchException(parameters.length, 4);
    }
    double a = parameters[0];
    double mu = parameters[1];
    double sigmaLeft = parameters[2];
    double sigmaRight = parameters[3];

    double[] grad = new double[4];
    double commonFactor = Math.pow(x - mu, 2);

    if (x <= mu) {
      if (sigmaLeft == 0) { // Avoid division by zero
        grad[0] = (x == mu) ? 1 : 0;
        grad[1] = 0;
        grad[2] = 0;
        grad[3] = 0;
        return grad;
      }
      double expTerm = Math.exp(-commonFactor / (2 * sigmaLeft * sigmaLeft));
      grad[0] = expTerm;                                          // dF/dA
      grad[1] = a * expTerm * (x - mu) / (sigmaLeft * sigmaLeft); // dF/dmu
      grad[2] = a * expTerm * commonFactor / Math.pow(sigmaLeft, 3); // dF/dsigma_left
      grad[3] = 0;                                                 // dF/dsigma_right
    } else {
      if (sigmaRight == 0) { // Avoid division by zero
        // This case is tricky if x > mu and sigmaRight = 0, y should be 0.
        // Derivatives would also be 0.
        grad[0] = 0;
        grad[1] = 0;
        grad[2] = 0;
        grad[3] = 0;
        return grad;
      }
      double expTerm = Math.exp(-commonFactor / (2 * sigmaRight * sigmaRight));
      grad[0] = expTerm;                                           // dF/dA
      grad[1] = a * expTerm * (x - mu) / (sigmaRight * sigmaRight); // dF/dmu
      grad[2] = 0;                                                  // dF/dsigma_left
      grad[3] = a * expTerm * commonFactor / Math.pow(sigmaRight, 3); // dF/dsigma_right
    }
    return grad;
  }

  @Override
  public @Nullable ParameterValidator getValidator() {
    return params -> {
      // Ensure sigmas are positive
      if (params.getEntry(2) <= 1e-9) {
        params.setEntry(2, 1e-9);
      } // sigmaLeft
      if (params.getEntry(3) <= 1e-9) {
        params.setEntry(3, 1e-9);
      } // sigmaRight
      // Optional: Ensure amplitude is positive for typical peaks
      // if (params.getEntry(0) < 0) { params.setEntry(0, 1e-9); }
      return params;
    };
  }

  @Override
  public double @Nullable [] guessStartParameters(List<WeightedObservedPoint> points) {
    return InitialGuess.guessBiGaussian(points);
  }

  @Override
  public FitQuality performFit(List<WeightedObservedPoint> points) {
    final double[] initialParametersGuess = this.guessStartParameters(points);
    if (initialParametersGuess == null) {
      return null;
    }

    try {
      final Optimum optimum = PeakFitterUtils.performFit(points, this, initialParametersGuess,
          getValidator());
      if (optimum == null) {
        return null;
      }

      final double[] paramsBiGaussian = optimum.getPoint().toArray();

      PeakShapeClassification peakShapeClassification = getPeakType();
      if (paramsBiGaussian[3] > paramsBiGaussian[2] * 1.05) {
        peakShapeClassification = PeakShapeClassification.TAILING_GAUSSIAN;
      } else if (paramsBiGaussian[2] > paramsBiGaussian[3] * 1.05) {
        peakShapeClassification = PeakShapeClassification.FRONTING_GAUSSIAN;
      }

      return PeakFitterUtils.calculateFitQuality(points, paramsBiGaussian, this,
          peakShapeClassification);
    } catch (final Exception e) {
      return null;
    }
  }

  @Override
  public PeakShapeClassification getPeakType() {
    return PeakShapeClassification.GAUSSIAN;
  }
}
