/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.AnyXYProvider;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.AbstractBaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.AbstractBaselineCorrectorParameters;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.IndexRange;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PolynomialBaselineCorrection extends AbstractBaselineCorrector {

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

  @Override
  public <T extends IntensityTimeSeries> T correctBaseline(T timeSeries) {
    additionalData.clear();
    final int numValues = timeSeries.getNumberOfValues();
    if (yBuffer.length < numValues) {
      xBuffer = new double[numValues];
      yBuffer = new double[numValues];
      xBufferRemovedPeaks = new double[numValues];
      yBufferRemovedPeaks = new double[numValues];
    }
    extractDataIntoBuffer(timeSeries, xBuffer, yBuffer);

    if (resolver != null) {
      // remove peaks
      final List<Range<Double>> resolved = resolver.resolve(xBuffer, yBuffer);
      final List<IndexRange> indices = resolved.stream().map(
          range -> BinarySearch.indexRange(range, timeSeries.getNumberOfValues(),
              timeSeries::getRetentionTime)).toList();
      final int numPointsInRemovedArray = AbstractBaselineCorrector.removeRangesFromArray(indices,
          numValues, xBuffer, xBufferRemovedPeaks);
      AbstractBaselineCorrector.removeRangesFromArray(indices, numValues, yBuffer,
          yBufferRemovedPeaks);

      final PolynomialFunction function = calculateFitFunction(xBufferRemovedPeaks,
          yBufferRemovedPeaks, numPointsInRemovedArray);
      if (isPreview()) {
        additionalData.add(new AnyXYProvider(Color.RED, "baseline", numValues, i -> xBuffer[i],
            i -> function.value(xBuffer[i])));
      }
      for (int i = 0; i < numValues; i++) {
        // must be above zero, but not bigger than the original value.
        yBuffer[i] = Math.min(Math.max(yBuffer[i] - function.value(xBuffer[i]), 0), yBuffer[i]);
      }

      return createNewTimeSeries(timeSeries, numValues, yBuffer);

    } else {

      final PolynomialFunction function = calculateFitFunction(xBuffer, yBuffer, numValues);
      if (isPreview()) {
        additionalData.add(new AnyXYProvider(Color.RED, "baseline", numValues, i -> xBuffer[i],
            i -> function.value(xBuffer[i])));
      }
      for (int i = 0; i < numValues; i++) {
        // must be above zero, but not bigger than the original value.
        yBuffer[i] = Math.min(Math.max(yBuffer[i] - function.value(xBuffer[i]), 0), yBuffer[i]);
      }

      return createNewTimeSeries(timeSeries, numValues, yBuffer);
    }
  }

  private @NotNull PolynomialFunction calculateFitFunction(double[] xValues, double[] yValues,
      int numValuesInArray) {
    final double[] subsampleX = BaselineCorrector.subsample(xValues, numValuesInArray, numSamples,
        true);
    final double[] subsampleY = BaselineCorrector.subsample(yValues, numValuesInArray, numSamples,
        false);

    var fitter = PolynomialCurveFitter.create(degree).withMaxIterations(iterations);
    List<WeightedObservedPoint> points = new ArrayList<>();
    for (int i = 0; i < subsampleX.length; i++) {
      final WeightedObservedPoint point = new WeightedObservedPoint(1, subsampleX[i],
          subsampleY[i]);
      points.add(point);
    }

    final double[] fit = fitter.fit(points);
    PolynomialFunction function = new PolynomialFunction(fit);
    return function;
  }

  @Override
  public BaselineCorrector newInstance(ParameterSet parameters,
      MemoryMapStorage storage, FeatureList flist) {
    final String suffix = parameters.getValue(BaselineCorrectionParameters.suffix);
    final ParameterSet embedded = parameters.getParameter(
        BaselineCorrectionParameters.correctionAlgorithm).getEmbeddedParameters();
    final Integer numSamples = embedded.getValue(AbstractBaselineCorrectorParameters.numSamples);
    final MinimumSearchFeatureResolver resolver =
        embedded.getValue(AbstractBaselineCorrectorParameters.applyPeakRemoval)
            ? AbstractBaselineCorrector.initializeLocalMinResolver((ModularFeatureList) flist)
            : null;
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
