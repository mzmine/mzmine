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

  private static final String MODULE_NAME = "3D visualizer";
  private static final String MODULE_DESCRIPTION = "3D visualizer."; // TODO

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {

    final RawDataFile[] currentDataFiles = parameters
        .getParameter(Fx3DVisualizerParameters.dataFiles).getValue().getMatchingRawDataFiles();

    final ScanSelection scanSel =
        parameters.getParameter(Fx3DVisualizerParameters.scanSelection).getValue();
    final List<Feature> featureSelList =
        parameters.getParameter(Fx3DVisualizerParameters.features).getValue();
    logger.finest("Feature selection is:" + featureSelList.toString());

    Range<Float> rtRange = ScanUtils.findRtRange(scanSel
        .getMatchingScans(MZmineCore.getProjectManager().getCurrentProject().getDataFiles()[0]));

    ParameterSet myParameters =
        MZmineCore.getConfiguration().getModuleParameters(Fx3DVisualizerModule.class);
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

  public static void setupNew3DVisualizer(final RawDataFile dataFile) {
    setupNew3DVisualizer(dataFile, null, null, null);
  }

  public static void setupNew3DVisualizer(final RawDataFile dataFile, final Range<Double> mzRange,
      final Range<Double> rtRange) {
    setupNew3DVisualizer(dataFile, null, null, null);
  }

  public static void setupNew3DVisualizer(final RawDataFile dataFile, final Range<Double> mzRange,
      final Range<Float> rtRange, final Feature featureToShow) {

    final ParameterSet myParameters =
        MZmineCore.getConfiguration().getModuleParameters(Fx3DVisualizerModule.class);
    final Fx3DVisualizerModule myInstance =
        MZmineCore.getModuleInstance(Fx3DVisualizerModule.class);
    myParameters.getParameter(Fx3DVisualizerParameters.dataFiles)
        .setValue(RawDataFilesSelectionType.SPECIFIC_FILES, new RawDataFile[] {dataFile});
    myParameters.getParameter(Fx3DVisualizerParameters.scanSelection)
        .setValue(new ScanSelection(rtRange, 1));
    myParameters.getParameter(Fx3DVisualizerParameters.mzRange).setValue(mzRange);
    myParameters.getParameter(Fx3DVisualizerParameters.features)
        .setValue(Collections.singletonList(featureToShow));

    if (myParameters.showSetupDialog(true) == ExitCode.OK) {
      myInstance.runModule(MZmineCore.getProjectManager().getCurrentProject(),
          myParameters.cloneParameterSet(), new ArrayList<Task>(), Instant.now());
    }
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
