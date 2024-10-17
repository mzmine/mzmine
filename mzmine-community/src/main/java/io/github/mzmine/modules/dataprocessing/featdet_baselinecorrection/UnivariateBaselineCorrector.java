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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.AnyXYProvider;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.util.MemoryMapStorage;
import java.awt.Color;
import java.util.List;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.math.plot.utils.Array;

public abstract class UnivariateBaselineCorrector extends AbstractBaselineCorrector {

  public UnivariateBaselineCorrector() {
    super(null, 5, "", null);
  }

  public UnivariateBaselineCorrector(@Nullable MemoryMapStorage storage, int numSamples,
      @NotNull String suffix, @Nullable MinimumSearchFeatureResolver resolver) {
    super(storage, numSamples, suffix, resolver);
  }

  /**
   * @param timeSeries
   * @param numValues
   * @param numPointsInRemovedArray
   * @param xBufferRemovedPeaks     might be the whole x data or peaks removed
   * @param yBufferRemovedPeaks     might be the whole y data or peaks removed
   * @param <T>
   * @return
   */
  protected <T extends IntensityTimeSeries> T subSampleAndCorrect(T timeSeries, int numValues,
      int numPointsInRemovedArray, double[] xBufferRemovedPeaks, double[] yBufferRemovedPeaks) {
    int stepSize = numSamples;
    var subsampleIndices = buffer.createSubSampleIndicesFromLandmarks(stepSize);

    final double[] subsampleX = BaselineCorrector.subsample(xBufferRemovedPeaks,
        numPointsInRemovedArray, subsampleIndices, true);
    final double[] subsampleY = BaselineCorrector.subsample(yBufferRemovedPeaks,
        numPointsInRemovedArray, subsampleIndices, false);

    UnivariateInterpolator interpolator = initializeInterpolator(subsampleX.length);
    UnivariateFunction splineFunction = interpolator.interpolate(subsampleX, subsampleY);

    for (int i = 0; i < numValues; i++) {
      // must be above zero, but not bigger than the original value.
      yBuffer()[i] = Math.min(Math.max(yBuffer()[i] - splineFunction.value(xBuffer()[i]), 0),
          yBuffer()[i]);
    }

    if (isPreview()) {
      additionalData.add(new AnyXYProvider(Color.RED, "baseline", numValues, j -> xBuffer()[j],
          j -> splineFunction.value(xBuffer()[j])));

      additionalData.add(
          new AnyXYProvider(Color.BLUE, "samples", subsampleY.length, j -> subsampleX[j],
              j -> subsampleY[j]));
    }

    return createNewTimeSeries(timeSeries, numValues, yBuffer());
  }

  @Override
  public <T extends IntensityTimeSeries> T correctBaseline(T timeSeries) {
    additionalData.clear();
    final int numValues = timeSeries.getNumberOfValues();
    buffer.extractDataIntoBuffer(timeSeries);

    if (resolver != null) {
      // resolver sets some data points to 0 if < chromatographic threshold
      final List<Range<Double>> resolved = resolver.resolve(Array.copy(xBuffer()),
          Array.copy(yBuffer()));
      final int numPointsInRemovedArray = buffer.removeRangesFromArrays(resolved);

      // use the data with features removed
      return subSampleAndCorrect(timeSeries, numValues, numPointsInRemovedArray,
          xBufferRemovedPeaks(), yBufferRemovedPeaks());
    } else {
      // use the original data
      return subSampleAndCorrect(timeSeries, numValues, numValues, xBuffer(), yBuffer());
    }
  }

  protected abstract UnivariateInterpolator initializeInterpolator(int actualNumberOfSamples);

}
