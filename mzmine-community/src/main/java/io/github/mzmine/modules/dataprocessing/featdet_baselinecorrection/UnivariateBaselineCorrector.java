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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.AnyXYProvider;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.IndexRange;
import java.awt.Color;
import java.util.List;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class UnivariateBaselineCorrector extends AbstractBaselineCorrector {

  public UnivariateBaselineCorrector() {
    super(null, 5, "", null);
  }

  public UnivariateBaselineCorrector(@Nullable MemoryMapStorage storage, int numSamples,
      @NotNull String suffix, @Nullable MinimumSearchFeatureResolver resolver) {
    super(storage, numSamples, suffix, resolver);
  }

  protected <T extends IntensityTimeSeries> T subSampleAndCorrect(T timeSeries, int numValues,
      int numPointsInRemovedArray, double[] xBufferRemovedPeaks, double[] yBufferRemovedPeaks) {
    final double[] subsampleX = BaselineCorrector.subsample(xBufferRemovedPeaks,
        numPointsInRemovedArray, numSamples, true);
    final double[] subsampleY = BaselineCorrector.subsample(yBufferRemovedPeaks,
        numPointsInRemovedArray, numSamples, false);

    UnivariateInterpolator interpolator = initializeInterpolator(subsampleX.length);
    UnivariateFunction splineFunction = interpolator.interpolate(subsampleX, subsampleY);

    for (int i = 0; i < numValues; i++) {
      // must be above zero, but not bigger than the original value.
      yBuffer[i] = Math.min(Math.max(yBuffer[i] - splineFunction.value(xBuffer[i]), 0), yBuffer[i]);
    }

    if (isPreview()) {
      additionalData.add(new AnyXYProvider(Color.RED, "baseline", numValues, j -> xBuffer[j],
          j -> splineFunction.value(xBuffer[j])));
    }

    return createNewTimeSeries(timeSeries, numValues, yBuffer);
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
      final List<Range<Double>> resolved = resolver.resolve(xBuffer, yBuffer);
      final List<IndexRange> indices = resolved.stream().map(
          range -> BinarySearch.indexRange(range, timeSeries.getNumberOfValues(),
              timeSeries::getRetentionTime)).toList();

      final int numPointsInRemovedArray = AbstractBaselineCorrector.removeRangesFromArray(indices,
          numValues, xBuffer, xBufferRemovedPeaks);
      AbstractBaselineCorrector.removeRangesFromArray(indices, numValues, yBuffer,
          yBufferRemovedPeaks);

      return subSampleAndCorrect(timeSeries, numValues, numPointsInRemovedArray,
          xBufferRemovedPeaks, yBufferRemovedPeaks);
    } else {
      return subSampleAndCorrect(timeSeries, numValues, numValues, xBuffer, yBuffer);
    }
  }

  protected abstract UnivariateInterpolator initializeInterpolator(int actualNumberOfSamples);

}
