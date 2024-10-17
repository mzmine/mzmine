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
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.AnyXYProvider;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.AbstractBaselineCorrectorParameters;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.AbstractResolverBaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.XYDataArrays;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import it.unimi.dsi.fastutil.ints.IntList;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PolynomialBaselineCorrection extends AbstractResolverBaselineCorrector {

  private final int degree;
  private final int iterations;

  public PolynomialBaselineCorrection() {
    super(null, 5, "", null);
    degree = 1;
    iterations = 1;
  }

  public PolynomialBaselineCorrection(MemoryMapStorage storage, int numSamples, String suffix,
      MinimumSearchFeatureResolver resolver, int degree, int iterations) {
    super(storage, numSamples, suffix, resolver);
    this.degree = degree;
    this.iterations = iterations;
  }


  /**
   * @param xDataToCorrect    the data to correct
   * @param yDataToCorrect    the data to correct
   * @param numValues         corresponding number of values - input arrays may be longer
   * @param xDataFiltered     might be the whole x data or peaks removed
   * @param yDataFiltered     might be the whole y data or peaks removed
   * @param numValuesFiltered number of filtered data points
   * @param addPreview        add preview datasets
   */
  @Override
  protected void subSampleAndCorrect(final double[] xDataToCorrect, final double[] yDataToCorrect,
      int numValues, double[] xDataFiltered, double[] yDataFiltered, int numValuesFiltered,
      final boolean addPreview) {
    // TODO change parameter to step size or window size or calculate from parameters
    int stepSize = numSamples;
    IntList subsampleIndices = buffer.createSubSampleIndicesFromLandmarks(stepSize);

    XYDataArrays subData = subSampleData(subsampleIndices, xDataFiltered, yDataFiltered,
        numValuesFiltered);

    var fitter = PolynomialCurveFitter.create(degree).withMaxIterations(iterations);
    List<WeightedObservedPoint> points = new ArrayList<>();
    for (int i = 0; i < subData.numValues(); i++) {
      final WeightedObservedPoint point = new WeightedObservedPoint(1, subData.getX(i),
          subData.getY(i));
      points.add(point);
    }

    final double[] fit = fitter.fit(points);
    PolynomialFunction function = new PolynomialFunction(fit);

    for (int i = 0; i < numValues; i++) {
      // must be above zero, but not bigger than the original value.
      yDataToCorrect[i] = Math.min(
          Math.max(yDataToCorrect[i] - function.value(xDataToCorrect[i]), 0), yDataToCorrect[i]);
    }

    if (addPreview) {
      additionalData.add(
          new AnyXYProvider(Color.BLUE, "samples", subData.numValues(), subData::getX,
              subData::getY));
      additionalData.add(new AnyXYProvider(Color.RED, "baseline", numValues, i -> xBuffer()[i],
          i -> function.value(xBuffer()[i])));
    }
  }


  @Override
  public BaselineCorrector newInstance(ParameterSet parameters, MemoryMapStorage storage,
      FeatureList flist) {
    final String suffix = parameters.getValue(BaselineCorrectionParameters.suffix);
    final ParameterSet embedded = parameters.getParameter(
        BaselineCorrectionParameters.correctionAlgorithm).getEmbeddedParameters();
    final Integer numSamples = embedded.getValue(AbstractBaselineCorrectorParameters.numSamples);
    final MinimumSearchFeatureResolver resolver =
        embedded.getValue(AbstractBaselineCorrectorParameters.applyPeakRemoval)
            ? initializeLocalMinResolver((ModularFeatureList) flist) : null;
    final Integer degree = embedded.getValue(PolynomialBaselineCorrectorParameters.degree);

    return new PolynomialBaselineCorrection(storage, numSamples, suffix, resolver, degree,
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
