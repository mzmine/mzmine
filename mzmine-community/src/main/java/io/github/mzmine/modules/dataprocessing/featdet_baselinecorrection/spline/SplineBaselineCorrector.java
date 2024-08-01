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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.spline;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineCorrector;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.IndexRange;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SplineBaselineCorrector implements BaselineCorrector {

  private final MinimumSearchFeatureResolver resolver;
  private final int numSamples;
  private double[] xBuffer = new double[0];
  private double[] yBuffer = new double[0];

  private double[] xBufferRemovedPeaks = new double[0];
  private double[] yBufferRemovedPeaks = new double[0];

  public SplineBaselineCorrector() {
    resolver = null;
    numSamples = 2;
  }

  public SplineBaselineCorrector(int numSamples, MinimumSearchFeatureResolver resolver) {
    this.numSamples = numSamples;
    this.resolver = resolver;
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

    final List<Range<Double>> resolved = resolver.resolve(xBuffer, yBuffer);
    final List<IndexRange> indices = resolved.stream().map(
        range -> BinarySearch.indexRange(range, timeSeries.getNumberOfValues(),
            timeSeries::getRetentionTime)).toList();
    final int sum = indices.stream().mapToInt(r -> r.maxExclusive() - r.min()).sum();

    for (int i = 0; i < indices.size(); i++) {

    }

    return null;
  }

  @Override
  public BaselineCorrector newInstance(BaselineCorrectionParameters parameters,
      MemoryMapStorage storage, FeatureList flist) {

    final ParameterSet resolverParam = ConfigService.getConfiguration()
        .getModuleParameters(MinimumSearchFeatureResolverModule.class).cloneParameterSet();
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.PEAK_LISTS,
        new FeatureListsSelection((ModularFeatureList) flist));
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters, false);
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.dimension,
        ResolvingDimension.RETENTION_TIME);
    resolverParam.setParameter(
        MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL, 0.70);
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.SEARCH_RT_RANGE, 0.04);
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.MIN_RELATIVE_HEIGHT, 0d);
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.MIN_ABSOLUTE_HEIGHT, 0d);
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.MIN_RATIO, 3d);
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.PEAK_DURATION,
        Range.closed(0d, 5d));
    resolverParam.setParameter(MinimumSearchFeatureResolverParameters.MIN_NUMBER_OF_DATAPOINTS, 5);

    final MinimumSearchFeatureResolver resolver = new MinimumSearchFeatureResolver(resolverParam,
        (ModularFeatureList) flist);

    final ParameterSet embedded = parameters.getParameter(
        BaselineCorrectionParameters.correctionAlgorithm).getEmbeddedParameters();
    return new SplineBaselineCorrector(
        embedded.getValue(SplineBaselineCorrectorParameters.numSamples), resolver);
  }

  @Override
  public @NotNull String getName() {
    return "Spline baseline correction";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return SplineBaselineCorrectorParameters.class;
  }
}
