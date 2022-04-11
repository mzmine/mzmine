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

package io.github.mzmine.modules.visualization.twod;

import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.scans.ScanUtils;

/**
 * 2D visualizer using JFreeChart library
 */
public class TwoDVisualizerModule implements MZmineRunnableModule {

  private static final String MODULE_NAME = "2D visualizer";
  private static final String MODULE_DESCRIPTION = "2D visualizer."; // TODO

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
    RawDataFile dataFiles[] = parameters.getParameter(TwoDVisualizerParameters.dataFiles).getValue()
        .getMatchingRawDataFiles();
    ScanSelection scanSel =
        parameters.getParameter(TwoDVisualizerParameters.scanSelection).getValue();
    Scan scans[] = scanSel.getMatchingScans(dataFiles[0]);
    Range<Float> rtRange = ScanUtils.findRtRange(scans);

    Range<Double> mzRange = parameters.getParameter(TwoDVisualizerParameters.mzRange).getValue();
    TwoDVisualizerTab newTab =
        new TwoDVisualizerTab(dataFiles[0], scans, rtRange, mzRange, parameters);

    //newWindow.show();
    MZmineCore.getDesktop().addTab(newTab);

    return ExitCode.OK;
  }

  public static void show2DVisualizerSetupDialog(RawDataFile dataFile) {
    show2DVisualizerSetupDialog(dataFile, null, null);
  }

  public static void show2DVisualizerSetupDialog(RawDataFile dataFile, Range<Double> mzRange,
      Range<Float> rtRange) {

    ParameterSet parameters =
        MZmineCore.getConfiguration().getModuleParameters(TwoDVisualizerModule.class);

    parameters.getParameter(TwoDVisualizerParameters.dataFiles)
        .setValue(RawDataFilesSelectionType.SPECIFIC_FILES, new RawDataFile[] {dataFile});

    if (rtRange != null)
      parameters.getParameter(TwoDVisualizerParameters.scanSelection)
          .setValue(new ScanSelection(rtRange, 1));
    if (mzRange != null)
      parameters.getParameter(TwoDVisualizerParameters.mzRange).setValue(mzRange);

    ExitCode exitCode = parameters.showSetupDialog(true);

    if (exitCode != ExitCode.OK)
      return;

    ScanSelection scanSel =
        parameters.getParameter(TwoDVisualizerParameters.scanSelection).getValue();
    Scan scans[] = scanSel.getMatchingScans(dataFile);
    rtRange = ScanUtils.findRtRange(scans);

    mzRange = parameters.getParameter(TwoDVisualizerParameters.mzRange).getValue();

    TwoDVisualizerTab newWindow =
        new TwoDVisualizerTab(dataFile, scans, rtRange, mzRange, parameters);

    //newWindow.show();
    MZmineCore.getDesktop().addTab(newWindow);
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONRAWDATA;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return TwoDVisualizerParameters.class;
  }

}
