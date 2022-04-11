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

package io.github.mzmine.modules.io.export_features_metaboanalyst;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.ExitCode;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class MetaboAnalystExportParameters extends SimpleParameterSet {

  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("comma-separated values", "*.csv") //
  );

  public static final FeatureListsParameter featureLists = new FeatureListsParameter(1);

  public static final FileNameParameter filename = new FileNameParameter("Filename",
      "Use pattern \"{}\" in the file name to substitute with feature list name. "
      + "(i.e. \"blah{}blah.csv\" would become \"blahSourceFeatureListNameblah.csv\"). "
      + "If the file already exists, it will be overwritten.",
      extensions, FileSelectionType.SAVE);

  public static final ComboParameter<UserParameter<?, ?>> groupParameter =
      new ComboParameter<UserParameter<?, ?>>("Grouping parameter",
          "Project parameter that will be used to obtain group information to each sample (e.g. control vs disease). Please set parameters in the Project/Set sample parameters menu.",
          new UserParameter[0]);

  public MetaboAnalystExportParameters() {
    super(new Parameter[] {featureLists, filename, groupParameter});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    UserParameter<?, ?> projectParams[] =
        MZmineCore.getProjectManager().getCurrentProject().getParameters();
    getParameter(MetaboAnalystExportParameters.groupParameter).setChoices(projectParams);

    return super.showSetupDialog(valueCheckRequired);
  }
}
