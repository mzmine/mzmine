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
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.ParameterValidator;
import org.jetbrains.annotations.Nullable;

/**
 * Models a double peak. Uses two gaussians, both with different amplitudes, centers, and sigmas.
 */
public class GaussianDoublePeak implements PeakModelFunction {

  public GaussianDoublePeak() {
    super();
  }

  /**
   * Parameters: [0]=A1, [1]=mu1, [2]=sigma1, [3]=A2, [4]=mu2, [5]=sigma2
   */
  @Override
  public double value(double x, double... parameters) {
    if (parameters == null) {
      throw new NullArgumentException();
    }
    if (parameters.length != 6) {
      throw new DimensionMismatchException(parameters.length, 6);
    }
    double a1 = parameters[0];
    double mu1 = parameters[1];
    double sigma1 = parameters[2];
    double a2 = parameters[3];
    double mu2 = parameters[4];
    double sigma2 = parameters[5];

    double term1 = 0;
    if (sigma1 != 0) {
      term1 = a1 * Math.exp(-Math.pow(x - mu1, 2) / (2 * sigma1 * sigma1));
    } else if (x == mu1) { // Delta function case for peak 1
      term1 = a1;
    }

    double term2 = 0;
    if (sigma2 != 0) {
      term2 = a2 * Math.exp(-Math.pow(x - mu2, 2) / (2 * sigma2 * sigma2));
    } else if (x == mu2) { // Delta function case for peak 2
      term2 = a2;
    }

    return term1 + term2;
  }

  /**
   * Gradient: partial derivatives with respect to A1, mu1, sigma1, A2, mu2, sigma2
   */
  @Override
  public double[] gradient(double x, double... parameters) {
    if (parameters == null) {
      throw new NullArgumentException();
    }
    if (parameters.length != 6) {
      throw new DimensionMismatchException(parameters.length, 6);
    }
    double a1 = parameters[0];
    double mu1 = parameters[1];
    double sigma1 = parameters[2];
    double a2 = parameters[3];
    double mu2 = parameters[4];
    double sigma2 = parameters[5];

    double[] grad = new double[6];

    // Derivatives for the first Gaussian component
    if (sigma1 == 0) {
      grad[0] = (x == mu1) ? 1 : 0; // d/dA1
      grad[1] = 0;                  // d/dmu1 (undefined, but LM avoids sigma=0 ideally)
      grad[2] = 0;                  // d/dsigma1
    } else {
      double expTerm1 = Math.exp(-Math.pow(x - mu1, 2) / (2 * sigma1 * sigma1));
      double commonFactor1 = Math.pow(x - mu1, 2);

      grad[0] = expTerm1;                                        // d/dA1
      grad[1] = a1 * expTerm1 * (x - mu1) / (sigma1 * sigma1);   // d/dmu1
      grad[2] = a1 * expTerm1 * commonFactor1 / Math.pow(sigma1, 3); // d/dsigma1
    }

    // Derivatives for the second Gaussian component
    if (sigma2 == 0) {
      grad[3] = (x == mu2) ? 1 : 0; // d/dA2
      grad[4] = 0;                  // d/dmu2
      grad[5] = 0;                  // d/dsigma2
    } else {
      double expTerm2 = Math.exp(-Math.pow(x - mu2, 2) / (2 * sigma2 * sigma2));
      double commonFactor2 = Math.pow(x - mu2, 2);

      grad[3] = expTerm2;                                        // d/dA2
      grad[4] = a2 * expTerm2 * (x - mu2) / (sigma2 * sigma2);   // d/dmu2
      grad[5] = a2 * expTerm2 * commonFactor2 / Math.pow(sigma2, 3); // d/dsigma2
    }

    return grad;
  }

  @Nullable
  public ParameterValidator getValidator() {
    return params -> {
      if (params.getEntry(0) < 0) {
        params.setEntry(0, 1e-9); // A1
      }
      if (params.getEntry(2) <= 1e-9) {
        params.setEntry(2, 1e-9); // sigma1
      }
      if (params.getEntry(3) < 0) {
        params.setEntry(3, 1e-9); // A2
      }
      if (params.getEntry(5) <= 1e-9) {
        params.setEntry(5, 1e-9); // sigma2
      }
      return params;
    };
  }

  @Override
  public double @Nullable [] guessStartParameters(List<WeightedObservedPoint> points) {
    return InitialGuess.guessDoubleGaussian(points);
  }

  @Override
  public PeakShapeClassification getPeakType() {
    return PeakShapeClassification.DOUBLE_GAUSSIAN;
  }
}
