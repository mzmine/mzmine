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
