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
      final double samplePercentage, @NotNull final String suffix,
      @Nullable final MinimumSearchFeatureResolver resolver) {
    super(storage, samplePercentage, suffix);
    this.resolver = resolver;
  }

  /**
   * Sub sample and correct input data.
   *
   * @param xDataToCorrect    in place change operation, the data to correct
   * @param yDataToCorrect    in place change operation, the data to correct
   * @param numValues         corresponding number of values - input arrays may be longer
   * @param xDataFiltered     might be the whole x data or peaks removed
   * @param yDataFiltered     might be the whole y data or peaks removed
   * @param numValuesFiltered number of filtered data points
   * @param addPreview        add preview datasets
   */
  protected abstract void subSampleAndCorrect(final double[] xDataToCorrect,
      final double[] yDataToCorrect, int numValues, double[] xDataFiltered, double[] yDataFiltered,
      int numValuesFiltered, final boolean addPreview);

  /**
   * Sub sample and correct input data. Input data was not filtered before otherwise use
   * {@link #subSampleAndCorrect(double[], double[], int, double[], double[], int, boolean)} and
   * define filtered data with missing ranges.
   *
   * @param xDataToCorrect in place change operation, the data to correct
   * @param yDataToCorrect in place change operation, the data to correct
   * @param numValues      corresponding number of values - input arrays may be longer
   * @param addPreview     add preview datasets
   */
  protected void subSampleAndCorrect(final double[] xDataToCorrect, final double[] yDataToCorrect,
      int numValues, final boolean addPreview) {
    // use the same data as input and filtered data
    subSampleAndCorrect(xDataToCorrect, yDataToCorrect, numValues, xDataToCorrect, yDataToCorrect,
        numValues, addPreview);
  }

  /**
   * Sub sample and correct input data. Input data may have been filtered before - then the filtered
   * data with missing ranges will be used for sub sampling
   *
   * @param buffer     data source
   * @param addPreview add preview datasets
   */
  protected void subSampleAndCorrect(final BaselineDataBuffer buffer, final boolean addPreview) {
    if (buffer.hasRemovedRanges()) {
      subSampleAndCorrect(buffer.xBuffer(), buffer.yBuffer(), buffer.numValues(),
          buffer.xBufferRemovedPeaks(), buffer.yBufferRemovedPeaks(), buffer.remaining(),
          addPreview);
    } else {
      subSampleAndCorrect(buffer.xBuffer(), buffer.yBuffer(), buffer.numValues(), addPreview);
    }
  }


  protected @NotNull MinimumSearchFeatureResolver initializeLocalMinResolver(
      ModularFeatureList flist) {

    final MinimumSearchFeatureResolver resolver = new MinimumSearchFeatureResolver(flist,
        ResolvingDimension.RETENTION_TIME, 0.5, 0.04, 0.005, 1, 2.5, Range.closed(0d, 50d), 5);

    return resolver;
  }

  @Override
  public <T extends IntensityTimeSeries> T correctBaseline(T timeSeries) {
    additionalData.clear();
    buffer.extractDataIntoBuffer(timeSeries);
    final int numValues = buffer.numValues();

    if (resolver != null) {
      // 1. remove baseline on a copy of original data
      double[] copyX = Arrays.copyOf(buffer.xBuffer(), numValues);
      double[] copyY = Arrays.copyOf(buffer.yBuffer(), numValues);

      // inplace baseline correct on copyX and Y
      subSampleAndCorrect(copyX, copyY, numValues, false);

      // 2. detect peaks and remove the ranges from the original data
      // resolver sets some data points to 0 if < chromatographic threshold
      final List<Range<Double>> resolved = resolver.resolve(copyX, copyY);
      // 3. remove baseline finally on original data
      // results stored in buffer
      buffer.removeRangesFromArrays(resolved);

      // use the data with features removed - correct data is automatically chosen from buffer
      subSampleAndCorrect(buffer, isPreview());
    } else {
      // use the original data for baseline correction
      subSampleAndCorrect(buffer, isPreview());
    }
    // yBuffer was changed in place
    return createNewTimeSeries(timeSeries, numValues, buffer.yBuffer());
  }
}
