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

package io.github.mzmine.modules.dataprocessing.featdet_smoothing;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.util.ExitCode;
import org.jetbrains.annotations.NotNull;

public class SmoothingParameters extends SimpleParameterSet {

  public static final ModuleOptionsEnumComboParameter<FeatureSmoothingOptions> smoothingAlgorithm = new ModuleOptionsEnumComboParameter<>(
      "Smoothing algorithm", "Please select a smoothing algorithm.", FeatureSmoothingOptions.LOESS);
  public static final FeatureListsParameter featureLists = new FeatureListsParameter();
  public static final OriginalFeatureListHandlingParameter handleOriginal = //
      new OriginalFeatureListHandlingParameter(false);

  public static final StringParameter suffix = new StringParameter("Suffix",
      "The suffix to be added to processed feature lists.", "sm");

  public SmoothingParameters() {
    super(createParams(Setup.FULL),
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_smoothing/smoothing.html");
  }

  public SmoothingParameters(Setup setup) {
    super(createParams(setup),
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_smoothing/smoothing.html");
  }

  private enum Setup {
    FULL, INTEGRATED
  }

  private static Parameter[] createParams(Setup setup) {
    return switch (setup) {
      case FULL -> new Parameter[]{featureLists, smoothingAlgorithm, handleOriginal, suffix};
      case INTEGRATED -> new Parameter[]{smoothingAlgorithm};
    };
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    ParameterSetupDialog dialog = new SmoothingSetupDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @NotNull
  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
