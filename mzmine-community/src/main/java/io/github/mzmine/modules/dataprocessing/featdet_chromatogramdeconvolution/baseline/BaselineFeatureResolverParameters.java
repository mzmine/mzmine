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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.baseline;

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverSetupDialog;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.util.ExitCode;

public class BaselineFeatureResolverParameters extends GeneralResolverParameters {

  public static final DoubleParameter MIN_PEAK_HEIGHT = new DoubleParameter("Min peak height",
      "Minimum acceptable peak height (absolute intensity)",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final DoubleRangeParameter PEAK_DURATION = new DoubleRangeParameter(
      "Peak duration range (min)", "Range of acceptable peak lengths",
      MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.0, 10.0));

  public static final DoubleParameter BASELINE_LEVEL = new DoubleParameter("Baseline level",
      "Level below which all data points of the chromatogram are removed (absolute intensity)",
      MZmineCore.getConfiguration().getIntensityFormat());

  public BaselineFeatureResolverParameters() {
    super(new Parameter[]{PEAK_LISTS, SUFFIX, handleOriginal, groupMS2Parameters, MIN_PEAK_HEIGHT,
        PEAK_DURATION, BASELINE_LEVEL, MIN_NUMBER_OF_DATAPOINTS},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_resolver_baseline/baseline-resolver.html");
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
    return new BaselineFeatureResolver();
  }
}
