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
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.IndexRange;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.jetbrains.annotations.NotNull;

public abstract class UnivariateBaselineCorrector implements BaselineCorrector {

  protected final MinimumSearchFeatureResolver resolver;
  protected final int numSamples;
  protected final MemoryMapStorage storage;
  protected final String suffix;
  protected double[] xBuffer = new double[0];
  protected double[] yBuffer = new double[0];
  protected double[] xBufferRemovedPeaks = new double[0];
  protected double[] yBufferRemovedPeaks = new double[0];
  protected boolean runResolver = false;

  public UnivariateBaselineCorrector() {
    resolver = null;
    numSamples = 2;
    suffix = "bl";
    storage = null;
  }

  public UnivariateBaselineCorrector(MemoryMapStorage storage, int numSamples, String suffix,
      MinimumSearchFeatureResolver resolver) {
    this.storage = storage;
    this.numSamples = numSamples;
    this.suffix = suffix;
    this.resolver = resolver;
    if (resolver != null) {
      runResolver = true;
    }
  }

  /**
   * Removes the given list of index ranges from the array, always keeping the first and last value
   * even if they are contained in one of the ranges.
   *
   * @param indices   The list of index ranges. May be empty.
   * @param numValues the number of values in the array. Can be used to limit the search if
   *                  {@param src} is a buffer.
   * @param src       The source to copy values frm
   * @param dst       The destination to write the new array to.
   * @return The number of values written to the array.
   */
  public static int removeRangesFromArray(List<IndexRange> indices, int numValues, double[] src,
      double[] dst) {
    int startInRemovedArray = 0;
    int lastEndPointInOriginalArray = 0;

    if (indices.isEmpty()) {
      System.arraycopy(src, 0, dst, 0, numValues);
      startInRemovedArray = numValues;
      return startInRemovedArray;
    } else {
      if (indices.getFirst().min() == 0) {
        dst[0] = src[0];
        startInRemovedArray++;
        lastEndPointInOriginalArray++;
      }

      for (int i = 0; i < indices.size(); i++) {
        final IndexRange range = indices.get(i);
        final int numPoints = range.min() - lastEndPointInOriginalArray;

        // in case the first range starts at 0 and the first point was copied manually, this condition is not met.
        if (numPoints > 0) {
          System.arraycopy(src, lastEndPointInOriginalArray, dst, startInRemovedArray, numPoints);
          startInRemovedArray += numPoints;
        }
        lastEndPointInOriginalArray = range.maxExclusive();
      }
    }

    // add last value
    if (indices.getLast().maxExclusive() >= numValues) {
      dst[startInRemovedArray] = src[numValues - 1];
      startInRemovedArray++;
    } else {
      // add values until the end
      System.arraycopy(src, lastEndPointInOriginalArray, dst, startInRemovedArray,
          numValues - lastEndPointInOriginalArray);
      startInRemovedArray += numValues - lastEndPointInOriginalArray;
    }
    return startInRemovedArray;
  }

  protected static @NotNull MinimumSearchFeatureResolver initializeLocalMinResolver(
      ModularFeatureList flist) {
    final ParameterSet resolverParam = ConfigService.getConfiguration()
        .getModuleParameters(MinimumSearchFeatureResolverModule.class).cloneParameterSet();
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.PEAK_LISTS,
        new FeatureListsSelection(flist));
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters, false);
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.dimension,
        ResolvingDimension.RETENTION_TIME);
    resolverParam.setParameter(
        MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL, 0.0);
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.SEARCH_RT_RANGE, 0.04);
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.MIN_RELATIVE_HEIGHT, 0d);
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.MIN_ABSOLUTE_HEIGHT, 0d);
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.MIN_RATIO, 3d);
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.PEAK_DURATION,
        Range.closed(0d, 5d));
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.MIN_NUMBER_OF_DATAPOINTS, 5);

    final MinimumSearchFeatureResolver resolver = new MinimumSearchFeatureResolver(resolverParam,
        flist);
    return resolver;
  }

  protected <T extends IntensityTimeSeries> T subSampleAndCorrect(T timeSeries, int numValues,
      int numPointsInRemovedArray, double[] xBufferRemovedPeaks, double[] yBufferRemovedPeaks) {
    final double[] subsampleX = BaselineCorrector.subsample(xBufferRemovedPeaks,
        numPointsInRemovedArray, numSamples, true);
    final double[] subsampleY = BaselineCorrector.subsample(yBufferRemovedPeaks,
        numPointsInRemovedArray, numSamples, false);

    UnivariateInterpolator interpolator = initializeInterpolator();
    UnivariateFunction splineFunction = interpolator.interpolate(subsampleX, subsampleY);
    for (int i = 0; i < numValues; i++) {
      yBuffer[i] = yBuffer[i] - splineFunction.value(xBuffer[i]);
    }

    return switch (timeSeries) {
      case IonSpectrumSeries<?> s -> (T) s.copyAndReplace(storage, s.getMzValues(new double[0]),
          Arrays.copyOfRange(yBuffer, 0, numValues));
      case OtherTimeSeries o -> (T) o.copyAndReplace(
          o.getTimeSeriesData().getOtherDataFile().getCorrespondingRawDataFile()
              .getMemoryMapStorage(), Arrays.copyOfRange(yBuffer, 0, numValues),
          o.getName() + " " + suffix);
      default -> throw new IllegalStateException(
          "Unexpected time series: " + timeSeries.getClass().getName());
    };
  }

  @Override
  public <T extends IntensityTimeSeries> T correctBaseline(T timeSeries) {
    final int numValues = timeSeries.getNumberOfValues();
    if (yBuffer.length < numValues) {
      xBuffer = new double[numValues];
      yBuffer = new double[numValues];
      xBufferRemovedPeaks = new double[numValues];
      yBufferRemovedPeaks = new double[numValues];
    }

    extractDataIntoBuffer(timeSeries, xBuffer, yBuffer);

    if (runResolver) {
      final List<Range<Double>> resolved = resolver.resolve(xBuffer, yBuffer);
      final List<IndexRange> indices = resolved.stream().map(
          range -> BinarySearch.indexRange(range, timeSeries.getNumberOfValues(),
              timeSeries::getRetentionTime)).toList();

      final int numPointsInRemovedArray = UnivariateBaselineCorrector.removeRangesFromArray(indices,
          numValues, xBuffer, xBufferRemovedPeaks);
      UnivariateBaselineCorrector.removeRangesFromArray(indices, numValues, yBuffer,
          yBufferRemovedPeaks);

      return subSampleAndCorrect(timeSeries, numValues, numPointsInRemovedArray,
          xBufferRemovedPeaks, yBufferRemovedPeaks);
    } else {
      return subSampleAndCorrect(timeSeries, numValues, numValues, xBuffer, yBuffer);
    }
  }

  protected abstract UnivariateInterpolator initializeInterpolator();

}
