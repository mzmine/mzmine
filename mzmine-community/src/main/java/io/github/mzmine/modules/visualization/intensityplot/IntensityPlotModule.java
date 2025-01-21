/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.intensityplot;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

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

    ParameterSet parameters = MZmineCore.getConfiguration()
        .getModuleParameters(IntensityPlotModule.class);

    parameters.getParameter(IntensityPlotParameters.featureList)
        .setValue(FeatureListsSelectionType.SPECIFIC_FEATURELISTS, new FeatureList[]{featureList});

//    parameters.getParameter(IntensityPlotParameters.dataFiles)
//        .setChoices(featureList.getRawDataFiles().toArray(RawDataFile[]::new));
//
//    parameters.getParameter(IntensityPlotParameters.dataFiles)
//        .setValue(featureList.getRawDataFiles().toArray(RawDataFile[]::new));

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
      FeatureListRow selectedRows[] = parameters.getParameter(IntensityPlotParameters.selectedRows)
          .getMatchingRows(featureList);
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
