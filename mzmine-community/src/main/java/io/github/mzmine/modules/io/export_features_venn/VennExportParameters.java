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

package io.github.mzmine.modules.io.export_features_venn;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.ExitCode;

public class VennExportParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter(1);

  public static final DirectoryParameter directory = new DirectoryParameter("Directory",
      "Choose a directory to export the feature list to.");

  public static final BooleanParameter exportManual = new BooleanParameter(
      "Export gap filled as detected",
      "If checked, gap filled features will be exported as detected, otherwise they will be marked as undetected.");

  public VennExportParameters() {
    super(new Parameter[]{flists, directory, exportManual});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    String message = "Exports a feature list to a csv that can be plotted as a venn diagram by"
        + " other software such as "
        + "<a href=\"https://analyticalsciencejournals.onlinelibrary.wiley.com/doi/10.1002/pmic.201400320\">VennDis</a>"
        + " or <a href=\"https://bioinfogp.cnb.csic.es/tools/venny/index.html\">VENNY</a>";

    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
