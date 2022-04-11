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

package io.github.mzmine.modules.visualization.intensityplot;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

/**
 * Feature intensity plot module
 */
public class IntensityPlotModule implements MZmineRunnableModule {

  private static final String MODULE_NAME = "Feature intensity plot";
  private static final String MODULE_DESCRIPTION = "Feature intensity plot."; // TODO

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    IntensityPlotTab newTab = new IntensityPlotTab(parameters);
    //newFrame.show();
    MZmineCore.getDesktop().addTab(newTab);
    return ExitCode.OK;
  }

  public static void showIntensityPlot(@NotNull MZmineProject project, FeatureList featureList,
      FeatureListRow rows[]) {

    ParameterSet parameters =
        MZmineCore.getConfiguration().getModuleParameters(IntensityPlotModule.class);

    parameters.getParameter(IntensityPlotParameters.featureList)
        .setValue(FeatureListsSelectionType.SPECIFIC_FEATURELISTS, new FeatureList[] {featureList});

    parameters.getParameter(IntensityPlotParameters.dataFiles)
        .setChoices(featureList.getRawDataFiles().toArray(RawDataFile[]::new));

    parameters.getParameter(IntensityPlotParameters.dataFiles)
        .setValue(featureList.getRawDataFiles().toArray(RawDataFile[]::new));

    parameters.getParameter(IntensityPlotParameters.selectedRows).setValue(rows);

    UserParameter<?, ?> projectParams[] = project.getParameters();
    Object xAxisSources[] = new Object[projectParams.length + 1];
    xAxisSources[0] = IntensityPlotParameters.rawDataFilesOption;

    for (int i = 0; i < projectParams.length; i++) {
      xAxisSources[i + 1] = new ParameterWrapper(projectParams[i]);
    }

    parameters.getParameter(IntensityPlotParameters.xAxisValueSource).setChoices(xAxisSources);

    ExitCode exitCode = parameters.showSetupDialog(true);

    if (exitCode == ExitCode.OK) {
      FeatureListRow selectedRows[] =
          parameters.getParameter(IntensityPlotParameters.selectedRows).getMatchingRows(featureList);
      if (selectedRows.length == 0) {
        MZmineCore.getDesktop().displayErrorMessage("No rows selected");
        return;
      }

      IntensityPlotTab newTab = new IntensityPlotTab(parameters.cloneParameterSet());
      //newFrame.show();
      MZmineCore.getDesktop().addTab(newTab);
    }

  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONFEATURELIST;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return IntensityPlotParameters.class;
  }

}
