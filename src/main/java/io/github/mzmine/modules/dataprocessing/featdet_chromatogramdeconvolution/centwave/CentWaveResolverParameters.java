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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.centwave;

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverSetupDialog;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.util.ExitCode;
import java.text.NumberFormat;

/**
 * Parameters used by CentWaveDetector.
 */
public class CentWaveResolverParameters extends GeneralResolverParameters {

  /**
   * Peak integration methods.
   */
  public enum PeakIntegrationMethod {

    UseSmoothedData("Use smoothed data", 1), UseRawData("Use raw data", 2);

    private final String name;
    private final int index;

    /**
     * Create the method.
     *
     * @param aName   name
     * @param anIndex index (as used by findPeaks.centWave)
     */
    PeakIntegrationMethod(final String aName, final int anIndex) {

      name = aName;
      index = anIndex;
    }

    @Override
    public String toString() {

      return name;
    }

    public int getIndex() {

      return index;
    }
  }

  public static final DoubleRangeParameter PEAK_DURATION =
      new DoubleRangeParameter("Peak duration range", "Range of acceptable peak lengths",
          MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.0, 10.0));

  public static final DoubleRangeParameter PEAK_SCALES = new DoubleRangeParameter("Wavelet scales",
      "Range wavelet widths (smallest, largest) in minutes",
      MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.25, 5.0));

  public static final DoubleParameter SN_THRESHOLD = new DoubleParameter("S/N threshold",
      "Signal to noise ratio threshold", NumberFormat.getNumberInstance(), 10.0, 0.0, null);

  public static final ComboParameter<PeakIntegrationMethod> INTEGRATION_METHOD =
      new ComboParameter<PeakIntegrationMethod>("Peak integration method",
          "Type of data used during peak reconstruction", PeakIntegrationMethod.values(),
          PeakIntegrationMethod.UseSmoothedData);

  public CentWaveResolverParameters() {

    super(new Parameter[]{PEAK_LISTS, SUFFIX, handleOriginal, groupMS2Parameters, SN_THRESHOLD,
        PEAK_SCALES, PEAK_DURATION, INTEGRATION_METHOD, RENGINE_TYPE, MIN_NUMBER_OF_DATAPOINTS},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_resolver_centwave/centwave-resolver.html");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    final FeatureResolverSetupDialog dialog =
        new FeatureResolverSetupDialog(valueCheckRequired, this, null);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public FeatureResolver getResolver() {
    return new CentWaveResolver();
  }
}
