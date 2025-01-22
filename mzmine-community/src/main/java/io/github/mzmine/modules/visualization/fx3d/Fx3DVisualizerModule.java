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

package io.github.mzmine.modules.visualization.fx3d;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.scans.ScanUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

/**
 * @author akshaj This class represents the module class of the Fx3DVisualizer.
 */
public class Fx3DVisualizerModule implements MZmineRunnableModule {

  private static final Logger logger = Logger.getLogger(Fx3DVisualizerModule.class.getName());

  private static final String MODULE_NAME = "3D plot";
  private static final String MODULE_DESCRIPTION = "3D plot shows retention time, m/z, and intensity of MS data.";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  public static void setupNew3DVisualizer(final RawDataFile dataFile, final Range<Double> mzRange,
      final Range<Float> rtRange, final Feature featureToShow) {

    final ParameterSet myParameters = MZmineCore.getConfiguration()
        .getModuleParameters(Fx3DVisualizerModule.class);
    final Fx3DVisualizerModule myInstance = MZmineCore.getModuleInstance(
        Fx3DVisualizerModule.class);
    myParameters.getParameter(Fx3DVisualizerParameters.dataFiles)
        .setValue(RawDataFilesSelectionType.SPECIFIC_FILES, new RawDataFile[]{dataFile});
    myParameters.getParameter(Fx3DVisualizerParameters.scanSelection)
        .setValue(new ScanSelection(1, rtRange));
    myParameters.getParameter(Fx3DVisualizerParameters.mzRange).setValue(mzRange);
    myParameters.getParameter(Fx3DVisualizerParameters.features)
        .setValue(Collections.singletonList(featureToShow));

    if (myParameters.showSetupDialog(true) == ExitCode.OK) {
      myInstance.runModule(ProjectService.getProjectManager().getCurrentProject(),
          myParameters.cloneParameterSet(), new ArrayList<Task>(), Instant.now());
    }
  }

  public static void setupNew3DVisualizer(final RawDataFile dataFile) {
    setupNew3DVisualizer(dataFile, null, null, null);
  }

  public static void setupNew3DVisualizer(final RawDataFile dataFile, final Range<Double> mzRange,
      final Range<Double> rtRange) {
    setupNew3DVisualizer(dataFile, null, null, null);
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {

    final RawDataFile[] currentDataFiles = parameters.getParameter(
        Fx3DVisualizerParameters.dataFiles).getValue().getMatchingRawDataFiles();

    final ScanSelection scanSel = parameters.getParameter(Fx3DVisualizerParameters.scanSelection)
        .getValue();
    final List<Feature> featureSelList = parameters.getParameter(Fx3DVisualizerParameters.features)
        .getValue();
    logger.finest("Feature selection is:" + featureSelList.toString());

    Range<Float> rtRange = ScanUtils.findRtRange(scanSel.getMatchingScans(
        ProjectService.getProjectManager().getCurrentProject().getDataFiles()[0]));

    ParameterSet myParameters = MZmineCore.getConfiguration()
        .getModuleParameters(Fx3DVisualizerModule.class);
    Range<Double> mzRange = myParameters.getParameter(Fx3DVisualizerParameters.mzRange).getValue();

    int rtRes = myParameters.getParameter(Fx3DVisualizerParameters.rtResolution).getValue();
    int mzRes = myParameters.getParameter(Fx3DVisualizerParameters.mzResolution).getValue();

    if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
      MZmineCore.getDesktop().displayErrorMessage("The platform does not provide 3D support.");
      return ExitCode.ERROR;
    }

    Fx3DVisualizerTab newTab = new Fx3DVisualizerTab(currentDataFiles, scanSel, rtRange, mzRange,
        rtRes, mzRes, featureSelList);

    MZmineCore.getDesktop().addTab(newTab);

    return ExitCode.OK;

  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONRAWDATA;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return Fx3DVisualizerParameters.class;
  }

}
