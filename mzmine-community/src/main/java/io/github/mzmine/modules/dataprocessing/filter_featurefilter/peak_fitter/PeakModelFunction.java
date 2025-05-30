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
import java.util.logging.Logger;
import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.ParameterValidator;
import org.jetbrains.annotations.Nullable;

public interface PeakModelFunction extends ParametricUnivariateFunction {

  static final Logger logger = Logger.getLogger(PeakModelFunction.class.getName());

  @Nullable ParameterValidator getValidator();

  /**
   * @return Start parameters for the {@link ParametricUnivariateFunction}. Null if no parameters
   * can be found, meaning the peak will not fit.
   */
  double @Nullable [] guessStartParameters(List<WeightedObservedPoint> points);

  default FitQuality performFit(List<WeightedObservedPoint> points) {
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

      double[] paramsGaussian = optimum.getPoint().toArray();
      return PeakFitterUtils.calculateFitQuality(points, paramsGaussian, this, getPeakType());
    } catch (final Exception e) {
//      logger.log(Level.SEVERE, e.getMessage(), e);
      return null;
    }
  }

  PeakShapeClassification getPeakType();
}
