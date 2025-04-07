/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.polynomial;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.AbstractBaselineCorrectorParameters;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.UnivariateBaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PolynomialBaselineCorrection extends UnivariateBaselineCorrector {

  private final int degree;
  private final int iterations;

  public PolynomialBaselineCorrection() {
    super(null, 5, "", null);
    degree = 1;
    iterations = 1;
  }

  public PolynomialBaselineCorrection(MemoryMapStorage storage, double samplePercentage,
      String suffix,
      MinimumSearchFeatureResolver resolver, int degree, int iterations) {
    super(storage, samplePercentage, suffix, resolver);
    this.degree = degree;
    this.iterations = iterations;
  }


  @Override
  protected UnivariateFunction initializeFunction(final double[] x, final double[] y) {
    var fitter = PolynomialCurveFitter.create(degree).withMaxIterations(iterations);
    List<WeightedObservedPoint> points = new ArrayList<>();
    for (int i = 0; i < x.length; i++) {
      final WeightedObservedPoint point = new WeightedObservedPoint(1, x[i], y[i]);
      points.add(point);
    }

    final double[] fit = fitter.fit(points);
    return new PolynomialFunction(fit);
  }


  @Override
  public BaselineCorrector newInstance(ParameterSet parameters, MemoryMapStorage storage,
      FeatureList flist) {
    final String suffix = parameters.getValue(BaselineCorrectionParameters.suffix);
    final ParameterSet embedded = parameters.getParameter(
        BaselineCorrectionParameters.correctionAlgorithm).getEmbeddedParameters();
    final Double samplePercentage = embedded.getValue(
        AbstractBaselineCorrectorParameters.samplePercentage);
    final MinimumSearchFeatureResolver resolver =
        embedded.getValue(AbstractBaselineCorrectorParameters.applyPeakRemoval)
            ? initializeLocalMinResolver((ModularFeatureList) flist) : null;
    final Integer degree = embedded.getValue(PolynomialBaselineCorrectorParameters.degree);

    return new PolynomialBaselineCorrection(storage, samplePercentage, suffix, resolver, degree,
        Integer.MAX_VALUE);
  }

  @Override
  public @NotNull String getName() {
    return "Polynomial baseline correction";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return PolynomialBaselineCorrectorParameters.class;
  }
}
