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

package io.github.mzmine.modules.dataprocessing.filter_featurefilter;

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.IntRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.ExitCode;
import org.jetbrains.annotations.NotNull;

public class FeatureFilterParameters extends SimpleParameterSet {

  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  public static final StringParameter SUFFIX =
      new StringParameter("Name suffix", "Suffix to be added to feature list name", "filtered");

  public static final OptionalParameter<DoubleRangeParameter> PEAK_DURATION =
      new OptionalParameter<>(
          new DoubleRangeParameter("Duration", "Permissible range of peak durations",
              MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.0, 10.0)));

  public static final OptionalParameter<DoubleRangeParameter> PEAK_AREA =
      new OptionalParameter<>(new DoubleRangeParameter("Area", "Permissible range of peak area",
          MZmineCore.getConfiguration().getIntensityFormat(), Range.closed(0.0, 10000000.0)));

  public static final OptionalParameter<DoubleRangeParameter> PEAK_HEIGHT =
      new OptionalParameter<>(new DoubleRangeParameter("Height", "Permissible range of peak height",
          MZmineCore.getConfiguration().getIntensityFormat(), Range.closed(0.0, 10000000.0)));

  public static final OptionalParameter<IntRangeParameter> PEAK_DATAPOINTS =
      new OptionalParameter<>(new IntRangeParameter("# data points",
          "Permissible range of the number of data points over a peak", false,
          Range.closed(8, 30)));

  public static final OptionalParameter<DoubleRangeParameter> PEAK_FWHM =
      new OptionalParameter<>(new DoubleRangeParameter("FWHM",
          "Permissible range of the full width half minimum (FWHM) for a peak",
          MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.0, 2.0)));

  public static final OptionalParameter<DoubleRangeParameter> PEAK_TAILINGFACTOR =
      new OptionalParameter<>(new DoubleRangeParameter("Tailing factor",
          "Permissible range of the tailing factor for a peak",
          MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.5, 2.0)));

  public static final OptionalParameter<DoubleRangeParameter> PEAK_ASYMMETRYFACTOR =
      new OptionalParameter<>(new DoubleRangeParameter("Asymmetry factor",
          "Permissible range of the asymmetry factor for a peak",
          MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.5, 2.0)));

  public static final BooleanParameter AUTO_REMOVE = new BooleanParameter(
      "Remove source feature list after filtering",
      "If checked, the original feature list will be removed leaving only the filtered version");

  public static final BooleanParameter MS2_Filter =
      new BooleanParameter("Keep only features with MS/MS scan",
          "If checked, the feature that don't contain MS2 scan will be removed.");

  public FeatureFilterParameters() {
    super(
        new Parameter[] {PEAK_LISTS, SUFFIX, PEAK_DURATION, PEAK_AREA, PEAK_HEIGHT, PEAK_DATAPOINTS,
            PEAK_FWHM, PEAK_TAILINGFACTOR, PEAK_ASYMMETRYFACTOR, MS2_Filter, AUTO_REMOVE},
        "https://mzmine.github.io/mzmine_documentation/module_docs/feature_filter/feature_filter.html");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    // Update the parameter choices
    UserParameter<?, ?> newChoices[] =
        MZmineCore.getProjectManager().getCurrentProject().getParameters();
    String[] choices;
    if (newChoices == null || newChoices.length == 0) {
      choices = new String[1];
      choices[0] = "No parameters defined";
    } else {
      choices = new String[newChoices.length + 1];
      choices[0] = "Ignore groups";
      for (int i = 0; i < newChoices.length; i++) {
        choices[i + 1] = "Filtering by " + newChoices[i].getName();
      }
    }

    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
