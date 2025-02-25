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

package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.ranges.ListDoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.util.ExitCode;
import java.util.ArrayList;

public class SpectralDeconvolutionGCParameters extends SimpleParameterSet {

  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();

  public static final ModuleOptionsEnumComboParameter<SpectralDeconvolutionAlgorithms> SPECTRAL_DECONVOLUTION_ALGORITHM = new ModuleOptionsEnumComboParameter<>(
      "Deconvolution algorithm", "Algorithm to use for spectral deconvolution and its parameters.",
      SpectralDeconvolutionAlgorithms.RT_GROUPING_AND_SHAPE_CORRELATION);

  public static final StringParameter SUFFIX = new StringParameter("Name suffix",
      "Suffix to be added to feature list name", "decon");

  public static final OriginalFeatureListHandlingParameter HANDLE_ORIGINAL = new OriginalFeatureListHandlingParameter(
      false);

  public static final OptionalParameter<ListDoubleRangeParameter> MZ_VALUES_TO_IGNORE = new OptionalParameter<>(
      new ListDoubleRangeParameter("Exclude m/z-values",
          "m/z-values to exclude as model feature. Values will be added to pseudo spectrum, yet not considered as representative feature in the feature list. Unless all values are excluded.",
          false, new ArrayList<>()), false);

  public SpectralDeconvolutionGCParameters() {
    super(new Parameter[]{FEATURE_LISTS, SPECTRAL_DECONVOLUTION_ALGORITHM, SUFFIX, HANDLE_ORIGINAL,
            MZ_VALUES_TO_IGNORE},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_spectraldeconvolutiongc/spectraldeconvolutiongc.html");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    if ((getParameters() == null) || (getParameters().length == 0)) {
      return ExitCode.OK;
    }
    SpectralDeconvolutionGCDialog dialog = new SpectralDeconvolutionGCDialog(valueCheckRequired,
        this);

    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public int getVersion() {
    return 2;
  }

}
