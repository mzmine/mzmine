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

import java.util.logging.Logger;
import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.fitting.leastsquares.ParameterValidator;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.DiagonalMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

public class PeakFitterUtils {

  private static final Logger logger = Logger.getLogger(PeakFitterUtils.class.getName());

  public static FitQuality fitPeakModels(double[] xData, double[] yData,
      List<PeakModelFunction> peakModels) {

    if(xData.length < 5) {
      // will not attempt to fit a peak with less than 5 points
      return null;
    }

    // This list will be used for everything
    List<WeightedObservedPoint> pointList = new ArrayList<>();
    for (int i = 0; i < xData.length; i++) {
      // Using weight 1.0 for all points as in the original example
      WeightedObservedPoint point = new WeightedObservedPoint(1.0, xData[i], yData[i]);
      pointList.add(point);
    }

    FitQuality bestFit = null;
    for (PeakModelFunction peakModel : peakModels) {
      final FitQuality thisFit = peakModel.performFit(pointList);
      if (bestFit == null) {
        bestFit = thisFit;
      } else if (thisFit != null && bestFit.rSquared() < thisFit.rSquared()) {
        bestFit = thisFit;
      }
    }

    return bestFit;
  }

  static Optimum performFit(List<WeightedObservedPoint> points, ParametricUnivariateFunction func,
      double[] initialGuess, ParameterValidator validator) {

    int numPoints = points.size();
    if (numPoints == 0) {
      throw new IllegalArgumentException("Point list cannot be empty.");
    }

    double[] xObs = new double[numPoints];
    double[] yObs = new double[numPoints];
    double[] wObs = new double[numPoints]; // Weights

    for (int i = 0; i < numPoints; i++) {
      WeightedObservedPoint p = points.get(i);
      xObs[i] = p.getX();
      yObs[i] = p.getY();
      wObs[i] = p.getWeight();
    }

    // Model function: maps parameters -> array of model y-values for each xObs
    MultivariateVectorFunction valueFunction = modelParameters -> {
      double[] modelY = new double[numPoints];
      for (int i = 0; i < numPoints; i++) {
        modelY[i] = func.value(xObs[i], modelParameters);
      }
      return modelY;
    };

    // Jacobian function: maps parameters -> matrix of partial derivatives
    // Each row i corresponds to xObs[i]
    // Each column j corresponds to parameter j
    // Element (i,j) is d(modelY_i) / d(parameter_j)
    MultivariateMatrixFunction jacobianFunction = modelParameters -> {
      int numParameters = modelParameters.length; // Or initialGuess.length
      double[][] jacobian = new double[numPoints][numParameters];
      for (int i = 0; i < numPoints; i++) {
        jacobian[i] = func.gradient(xObs[i], modelParameters);
      }
      return new Array2DRowRealMatrix(jacobian,
          false).getDataRef(); // 'false' to avoid copying if jacobian array is not reused
    };

    // Construct the weight matrix for the optimizer if weights are not all 1.0
    // The optimizer minimizes 0.5 * sum(w_i * r_i^2).
    // So, w_i here are the actual weights.
    RealMatrix weightMatrix = null;
    boolean allWeightsOne = true;
    for (double w : wObs) {
      if (Math.abs(w - 1.0) > 1e-9) { // Check if any weight is not 1.0
        allWeightsOne = false;
        break;
      }
    }
    if (!allWeightsOne) {
      weightMatrix = new DiagonalMatrix(wObs);
    }
    // If all weights are 1.0, passing null to .weight() implies unweighted (or equally weighted).

    LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();

    LeastSquaresProblem problem = new LeastSquaresBuilder().start(
            initialGuess)                       // Initial guess for parameters
        .model(valueFunction, jacobianFunction)     // Model function and its Jacobian
        .target(yObs)                       // Observed y-values (the target for the model)
        .weight(weightMatrix) // Weights for residuals (null if all 1.0 or unweighted)
        .lazyEvaluation(false)// Eagerly compute value and Jacobian
        .maxEvaluations(100) // Max number of model evaluations
        .maxIterations(100)   // Max number of optimizer iterations
        .parameterValidator(validator)     // Optional parameter validator
        .build();

    try {
      return optimizer.optimize(problem);
    } catch (TooManyEvaluationsException e) {
      return null;
    }
  }

  static FitQuality calculateFitQuality(List<WeightedObservedPoint> observedPoints,
      // ParametricUnivariateFunction fittedFunction, // This parameter is not needed
      double[] fittedParameters, // Used for numParameters
      ParametricUnivariateFunction modelFunction, PeakShapeClassification peakShapeClassification) {
    final int numPoints = observedPoints.size();
    final int numParameters = fittedParameters.length; // Get number of parameters

    final double[] fittedYValues = observedPoints.stream()
        .mapToDouble(p -> modelFunction.value(p.getX(), fittedParameters)).toArray();

    final PearsonsCorrelation corr = new PearsonsCorrelation();
    final double r2 = corr.correlation(
        observedPoints.stream().mapToDouble(WeightedObservedPoint::getY).toArray(), fittedYValues);

    // Instantiate the FitQuality record using the 5-argument convenience constructor
    return new FitQuality(r2, numPoints,
        numParameters, fittedYValues, peakShapeClassification);
  }
}
