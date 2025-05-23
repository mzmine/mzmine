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

/**
 * A record to hold various goodness-of-fit metrics.
 *
 * @param rSquared         Coefficient of Determination.
 * @param fitScore         Penalized score of the fit.
 * @param numPoints        Number of data points.
 * @param numParameters    Number of fitted parameters.
 * @param degreesOfFreedom Calculated as numPoints - numParameters.
 */
public record FitQuality(double rSquared, double fitScore, int numPoints, int numParameters,
                         int degreesOfFreedom, double[] fittedY,
                         PeakShapeClassification peakShapeClassification) {

  /**
   * Convenience constructor that calculates derived metrics (degreesOfFreedom, chiSquared,
   * reducedChiSquared) from the primary metrics and data/parameter counts.
   */
  public FitQuality(double rSquared, int numPoints, int numParameters, double[] fittedY,
      PeakShapeClassification peakShapeClassification) {
    this(rSquared, rSquared * peakShapeClassification.getPenaltyFactor(), numPoints, numParameters,
        Math.max(1, numPoints - numParameters), fittedY, peakShapeClassification);
  }
}
