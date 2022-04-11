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

package io.github.mzmine.modules.visualization.scatterplot;

import io.github.mzmine.datamodel.features.FeatureList;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

public class ScatterPlotVisualizerModule implements MZmineRunnableModule {

  private static final String MODULE_NAME = "Scatter plot";
  private static final String MODULE_DESCRIPTION = "Scatter plot."; // TODO

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

    FeatureList featureLists[] =
        parameters.getParameter(ScatterPlotParameters.featureLists).getValue().getMatchingFeatureLists();
    if ((featureLists == null) || (featureLists.length != 1)) {
      MZmineCore.getDesktop().displayErrorMessage("Please select a single aligned feature list");
      return ExitCode.ERROR;
    }

    FeatureList featureList = featureLists[0];
    if (featureList.getNumberOfRawDataFiles() < 2) {
      MZmineCore.getDesktop().displayErrorMessage("There is only one raw data file in the selected "
          + "feature list, it is necessary at least two for comparison");
      return ExitCode.ERROR;
    }

    ScatterPlotTab newTab = new ScatterPlotTab(featureList);
    MZmineCore.getDesktop().addTab(newTab);

    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONFEATURELIST;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return ScatterPlotParameters.class;
  }

  public static void showNewScatterPlotWindow(FeatureList featureList) {

    if (featureList.getNumberOfRawDataFiles() < 2) {
      MZmineCore.getDesktop().displayErrorMessage("There is only one raw data file in the selected "
          + "feature list, it is necessary at least two for comparison");
      return;
    }

    ScatterPlotTab newTab = new ScatterPlotTab(featureList);
    MZmineCore.getDesktop().addTab(newTab);

  }

}
