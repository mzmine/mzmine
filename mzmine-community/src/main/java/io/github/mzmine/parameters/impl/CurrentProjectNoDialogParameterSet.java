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

package io.github.mzmine.parameters.impl;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.util.ExitCode;

/**
 * A parameter set that uses the currently selected project feature list and skips the setup dialog.
 * Can be used ot directly open tabs for feature lists or raw files
 */
public class CurrentProjectNoDialogParameterSet extends SimpleParameterSet {

  public CurrentProjectNoDialogParameterSet(final Parameter<?>[] parameters,
      final String onlineHelpUrl) {
    super(parameters, onlineHelpUrl);
  }

  public CurrentProjectNoDialogParameterSet(final String onlineHelpUrl,
      final Parameter<?>... parameters) {
    super(onlineHelpUrl, parameters);
  }

  public CurrentProjectNoDialogParameterSet(final Parameter<?>... parameters) {
    super(parameters);
  }

  @Override
  public ExitCode showSetupDialog(final boolean valueCheckRequired) {
//    var selectedFiles = MZmineGUI.getSelectedRawDataFiles();
//    var selectedFeatureLists = MZmineGUI.getSelectedFeatureLists();

    streamForClass(RawDataFilesParameter.class).forEach(
        params -> params.setValue(RawDataFilesSelectionType.GUI_SELECTED_FILES));

    streamForClass(FeatureListsParameter.class).forEach(
        params -> params.setValue(FeatureListsSelectionType.GUI_SELECTED_FEATURELISTS));

    return ExitCode.OK;
  }
}
