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

package io.github.mzmine.modules.dataanalysis.heatmaps;

import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import java.util.ArrayList;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.R.REngineType;

public class HeatMapParameters extends SimpleParameterSet {

  public static final String[] fileTypes = {"pdf", "svg", "png", "fig"};

  public static final FeatureListsParameter featureLists = new FeatureListsParameter(1, 1);

  public static final FileNameParameter fileName = new FileNameParameter("Output name",
      "Select the path and name of the output file.", FileSelectionType.SAVE);

  public static final ComboParameter<String> fileTypeSelection =
      new ComboParameter<String>("Output file type", "Output file type", fileTypes, fileTypes[0]);

  public static final ComboParameter<UserParameter<?, ?>> selectionData =
      new ComboParameter<UserParameter<?, ?>>("Sample parameter",
          "One sample parameter has to be selected to be used in the heat map. They can be defined in \"Project -> Set sample parameters\"",
          new UserParameter[0]);

  public static final ComboParameter<Object> referenceGroup =
      new ComboParameter<Object>("Group of reference",
          "Name of the group that will be used as a reference from the sample parameters",
          new Object[0]);

  public static final BooleanParameter useIdenfiedRows =
      new BooleanParameter("Only identified rows", "Plot only identified rows.", false);

  public static final BooleanParameter useFeatureArea = new BooleanParameter("Use feature area",
      "Feature area will be used if this option is selected. Feature height will be used otherwise",
      true);

  public static final BooleanParameter scale = new BooleanParameter("Scaling",
      "Scaling the data with the standard deviation of each column.", true);

  public static final BooleanParameter log =
      new BooleanParameter("Log", "Log scaling of the data", true);

  public static final BooleanParameter plegend = new BooleanParameter("P-value legend",
      "Adds the p-value legend and groups the data showing only the different groups in the heat map",
      true);

  public static final IntegerParameter star =
      new IntegerParameter("Size p-value legend", "Size of the p-value legend", 5);

  public static final BooleanParameter showControlSamples = new BooleanParameter(
      "Show control samples", "Shows control samples if this option is selected", true);

  public static final IntegerParameter height = new IntegerParameter("Height",
      "Height of the heat map. It has to be more than 500 if \"png\" has been choosen as an output format",
      10);

  public static final IntegerParameter width = new IntegerParameter("Width",
      "Width of the heat map. It has to be more than 500 if \"png\" has been choosen as an output format",
      10);

  public static final IntegerParameter columnMargin =
      new IntegerParameter("Column margin", "Column margin of the heat map", 10);

  public static final IntegerParameter rowMargin =
      new IntegerParameter("Row margin", "Row margin of the heat map", 10);

  /**
   * R engine type.
   */
  public static final ComboParameter<REngineType> RENGINE_TYPE = new ComboParameter<REngineType>(
      "R engine", "The R engine to be used for communicating with R.", REngineType.values(),
      REngineType.RCALLER);

  public HeatMapParameters() {
    super(new Parameter[] {featureLists, fileName, fileTypeSelection, selectionData, referenceGroup,
        useIdenfiedRows, useFeatureArea, scale, log, showControlSamples, plegend, star, height, width,
        columnMargin, rowMargin, RENGINE_TYPE});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    // Update the parameter choices
    MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
    UserParameter<?, ?> newChoices[] = project.getParameters();
    getParameter(HeatMapParameters.selectionData).getChoices().clear();
    getParameter(HeatMapParameters.selectionData).getChoices().addAll(newChoices);
    if (newChoices.length > 0) {
      ArrayList<Object> values = new ArrayList<Object>();
      for (RawDataFile dataFile : project.getDataFiles()) {
        Object paramValue = project.getParameterValue(newChoices[0], dataFile);
        if (paramValue == null) {
          continue;
        }
        if (!values.contains(paramValue)) {
          values.add(paramValue);
        }
      }
      getParameter(HeatMapParameters.referenceGroup).getChoices().clear();
      getParameter(HeatMapParameters.referenceGroup).getChoices().addAll(values);
    }
    HeatmapSetupDialog dialog = new HeatmapSetupDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
