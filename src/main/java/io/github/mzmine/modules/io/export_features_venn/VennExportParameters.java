/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
