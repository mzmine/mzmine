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

package io.github.mzmine.modules.dataprocessing.otherdata.featdet_resolve;

import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters.MIN_NUMBER_OF_DATAPOINTS;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters.dimension;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters.MIN_ABSOLUTE_HEIGHT;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters.MIN_RATIO;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters.MIN_RELATIVE_HEIGHT;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters.PEAK_DURATION;
import static io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters.SEARCH_RT_RANGE;

import com.google.common.collect.Range;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherTraceSelection;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherTraceSelectionParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import java.text.DecimalFormat;

public class OtherDataResolverParameters extends SimpleParameterSet {

  public static final OtherTraceSelectionParameter otherTraces = new OtherTraceSelectionParameter(
      "Select traces", "Select the traces you want to process.", OtherTraceSelection.rawUv());

  public static final PercentParameter chromThreshold = new PercentParameter(
      CHROMATOGRAPHIC_THRESHOLD_LEVEL.getName(), CHROMATOGRAPHIC_THRESHOLD_LEVEL.getDescription(),
      0.85, 0d, 1d);

  public static final DoubleParameter searchRange = new DoubleParameter(SEARCH_RT_RANGE.getName(),
      SEARCH_RT_RANGE.getDescription(), new DecimalFormat("0.000"), 0.05);

  public static final PercentParameter minRelHeight = new PercentParameter(
      MIN_RELATIVE_HEIGHT.getName(), MIN_RELATIVE_HEIGHT.getDescription());

  public static final DoubleParameter minAbsHeight = new DoubleParameter(
      MIN_ABSOLUTE_HEIGHT.getName(), MIN_ABSOLUTE_HEIGHT.getDescription(), new DecimalFormat("0.0"),
      10d);

  public static final DoubleParameter minRatio = new DoubleParameter(MIN_RATIO.getName(),
      MIN_RATIO.getDescription(), new DecimalFormat("0.00"), 1.7d);

  public static final DoubleRangeParameter peakDuration = new DoubleRangeParameter(
      PEAK_DURATION.getName(), PEAK_DURATION.getDescription(),
      ConfigService.getGuiFormats().rtFormat(), Range.closed(0.0, 10.0));

  public static final IntegerParameter minPoints = new IntegerParameter(
      MIN_NUMBER_OF_DATAPOINTS.getName(), MIN_NUMBER_OF_DATAPOINTS.getDescription(), 10);


  public OtherDataResolverParameters() {
    super(otherTraces, chromThreshold, searchRange, minRelHeight, minAbsHeight, minRatio,
        peakDuration, minPoints);
  }

  public ParameterSet toResolverParameters() {
    final ParameterSet param = ConfigService.getConfiguration()
        .getModuleParameters(MinimumSearchFeatureResolverModule.class).cloneParameterSet();

    param.setParameter(CHROMATOGRAPHIC_THRESHOLD_LEVEL, getValue(chromThreshold));
    param.setParameter(SEARCH_RT_RANGE, getValue(searchRange));
    param.setParameter(MIN_RELATIVE_HEIGHT, getValue(minRelHeight));
    param.setParameter(MIN_ABSOLUTE_HEIGHT, getValue(minAbsHeight));
    param.setParameter(MIN_RATIO, getValue(minRatio));
    param.setParameter(PEAK_DURATION, getValue(peakDuration));
    param.setParameter(MIN_NUMBER_OF_DATAPOINTS, getValue(minPoints));

    param.setParameter(dimension, ResolvingDimension.RETENTION_TIME);

    return param;
  }
}
