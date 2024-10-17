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
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Baseline correction that uses feature resolver as intermediate step
 */
public abstract class AbstractResolverBaselineCorrector extends AbstractBaselineCorrector {

  protected final MinimumSearchFeatureResolver resolver;

  public AbstractResolverBaselineCorrector(@Nullable final MemoryMapStorage storage,
      final int numSamples, @NotNull final String suffix,
      @Nullable final MinimumSearchFeatureResolver resolver) {
    super(storage, numSamples, suffix);
    this.resolver = resolver;
  }

  /**
   * Sub sample and correct input data.
   *
   * @param xDataToCorrect    the data to correct
   * @param yDataToCorrect    the data to correct
   * @param numValues         corresponding number of values - input arrays may be longer
   * @param xDataFiltered     might be the whole x data or peaks removed
   * @param yDataFiltered     might be the whole y data or peaks removed
   * @param numValuesFiltered number of filtered data points
   * @param addPreview        add preview datasets
   */
  protected abstract void subSampleAndCorrect(final double[] xDataToCorrect,
      final double[] yDataToCorrect, int numValues, double[] xDataFiltered, double[] yDataFiltered,
      int numValuesFiltered, final boolean addPreview);


  protected @NotNull MinimumSearchFeatureResolver initializeLocalMinResolver(
      ModularFeatureList flist) {

    final MinimumSearchFeatureResolver resolver = new MinimumSearchFeatureResolver(flist,
        ResolvingDimension.RETENTION_TIME, 0.5, 0.04, 0.005, 1, 2.5, Range.closed(0d, 50d), 5);

    return resolver;
  }

  @Override
  public <T extends IntensityTimeSeries> T correctBaseline(T timeSeries) {
    additionalData.clear();
    final int numValues = timeSeries.getNumberOfValues();
    buffer.extractDataIntoBuffer(timeSeries);

    if (resolver != null) {
      // 1. remove baseline on a copy
      double[] copyX = Arrays.copyOf(xBuffer(), numValues);
      double[] copyY = Arrays.copyOf(yBuffer(), numValues);

      // inplace baseline correct on copyX and Y
      subSampleAndCorrect(copyX, copyY, numValues, copyX, copyY, numValues, false);

      // 2. detect peaks and remove the ranges from the original data
      // resolver sets some data points to 0 if < chromatographic threshold
      final List<Range<Double>> resolved = resolver.resolve(copyX, copyY);
      // 3. remove baseline finally on original data
      final int numPointsInRemovedArray = buffer.removeRangesFromArrays(resolved);

      // use the data with features removed
      subSampleAndCorrect(xBuffer(), yBuffer(), numValues, xBufferRemovedPeaks(),
          yBufferRemovedPeaks(), numPointsInRemovedArray, isPreview());
    } else {
      // use the original data
      subSampleAndCorrect(xBuffer(), yBuffer(), numValues, xBuffer(), yBuffer(), numValues,
          isPreview());
    }
    return createNewTimeSeries(timeSeries, numValues, yBuffer());
  }
}
