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

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.util.ExitCode;

public class SpectralDeconvolutionGCParameters extends SimpleParameterSet {

  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();

  public static final RTToleranceParameter RT_TOLERANCE = new RTToleranceParameter();
  public static final IntegerParameter MIN_NUMBER_OF_SIGNALS = new IntegerParameter(
      "Minimum signals in pseudo spectrum",
      "Minimum number of deconvoluted signals in pseudo spectrum", 10, true, 1, 5000);
  public static final ComboParameter<SpectralDeconvolutionAlgorithm> SPECTRAL_DECONVOLUTION_ALGORITHM = new ComboParameter<>(
      "Deconvolution algorithm", "Choose the deconvolution algorithm",
      SpectralDeconvolutionAlgorithm.values());

  public static final StringParameter SUFFIX = new StringParameter("Name suffix",
      "Suffix to be added to feature list name", "decon");

  public static final OriginalFeatureListHandlingParameter HANDLE_ORIGINAL = new OriginalFeatureListHandlingParameter(
      false);

  public SpectralDeconvolutionGCParameters() {
    super(FEATURE_LISTS, RT_TOLERANCE, MIN_NUMBER_OF_SIGNALS, SPECTRAL_DECONVOLUTION_ALGORITHM,
        SUFFIX, HANDLE_ORIGINAL);
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
