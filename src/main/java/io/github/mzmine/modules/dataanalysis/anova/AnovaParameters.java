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

package io.github.mzmine.modules.dataanalysis.anova;

import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.util.ExitCode;

public class AnovaParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter(1, 1);

  public static final ComboParameter<UserParameter<?, ?>> selectionData =
      new ComboParameter<UserParameter<?, ?>>("Sample parameter",
          "One sample parameter has to be selected to be used in the test calculation. They can be defined in \"Project -> Set sample parameters\"",
          new UserParameter[0]);

  public AnovaParameters() {
    super(new Parameter[] {featureLists, selectionData});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    // Update the parameter choices
    MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
    UserParameter[] newChoices = project.getParameters();
    getParameter(AnovaParameters.selectionData).setChoices(newChoices);

    // Add a message
    String message = "<html>To view the results of ANOVA test, export the feature list to CSV file "
        + "and look for column ANOVA_P_VALUE. Click Help for details.</html>";

    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
