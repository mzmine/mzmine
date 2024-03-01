/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverSetupDialog;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.Resolver;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.util.ExitCode;
import java.text.DecimalFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MinimumSearchFeatureResolverParameters extends GeneralResolverParameters {

  public static final PercentParameter CHROMATOGRAPHIC_THRESHOLD_LEVEL = new PercentParameter(
      "Chromatographic threshold", "Percentile threshold for removing noise.\n"
      + "The algorithm will remove the lowest abundant X % data points from a chromatogram and only consider\n"
      + "the remaining (highest) values. Important filter for noisy chromatograms.", 0.85d, 0d, 1d);

  public static final DoubleParameter SEARCH_RT_RANGE = new DoubleParameter(
      "Minimum search range RT/Mobility (absolute)",
      "If a local minimum is minimal in this range of retention time or mobility, it will be considered a border between two peaks.\n"
          + "Start optimising with a value close to the FWHM of a peak.",
      new DecimalFormat("0.000"), 0.05);

  public static final PercentParameter MIN_RELATIVE_HEIGHT = new PercentParameter(
      "Minimum relative height",
      "Minimum height of a peak relative to the chromatogram top data point", 0d);

  public static final DoubleParameter MIN_ABSOLUTE_HEIGHT = new DoubleParameter(
      "Minimum absolute height", "Minimum absolute height of a peak to be recognized",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E3);

  public static final DoubleParameter MIN_RATIO = new DoubleParameter("Min ratio of peak top/edge",
      "Minimum ratio between peak's top intensity and side (lowest) data points."
          + "\nThis parameter helps to reduce detection of false peaks in case the chromatogram is not smooth.",
      new DecimalFormat("0.00"), 1.7d);

  public static final DoubleRangeParameter PEAK_DURATION = new DoubleRangeParameter(
      "Peak duration range (min/mobility)", "Range of acceptable peak lengths",
      MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.0, 10.0));

  public MinimumSearchFeatureResolverParameters() {
    super(createParams(Setup.FULL), "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_resolver_local_minimum/local-minimum-resolver.html");
  }

  public MinimumSearchFeatureResolverParameters(Setup setup) {
    super(createParams(setup), "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_resolver_local_minimum/local-minimum-resolver.html");
  }

  private static Parameter[] createParams(Setup setup) {
    return switch (setup) {
      case FULL -> new Parameter[]{PEAK_LISTS, SUFFIX, handleOriginal, groupMS2Parameters,
          dimension, CHROMATOGRAPHIC_THRESHOLD_LEVEL, SEARCH_RT_RANGE, MIN_RELATIVE_HEIGHT,
          MIN_ABSOLUTE_HEIGHT, MIN_RATIO, PEAK_DURATION, MIN_NUMBER_OF_DATAPOINTS};
      case INTEGRATED -> new Parameter[]{CHROMATOGRAPHIC_THRESHOLD_LEVEL, SEARCH_RT_RANGE,
          MIN_RELATIVE_HEIGHT, MIN_ABSOLUTE_HEIGHT, MIN_RATIO, PEAK_DURATION,
          MIN_NUMBER_OF_DATAPOINTS};
    };
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    final FeatureResolverSetupDialog dialog = new FeatureResolverSetupDialog(valueCheckRequired,
        this, null);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public FeatureResolver getResolver() {
    throw new UnsupportedOperationException("Legacy resolver method. Unsupported in local min.");
  }

  @Nullable
  @Override
  public Resolver getResolver(ParameterSet parameters, ModularFeatureList flist) {
    return new MinimumSearchFeatureResolver(parameters, flist);
  }

  @NotNull
  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  private enum Setup {
    FULL, INTEGRATED;
  }
}
