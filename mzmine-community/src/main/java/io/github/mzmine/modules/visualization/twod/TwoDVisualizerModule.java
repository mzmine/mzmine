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

package io.github.mzmine.modules.visualization.twod;

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
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/**
 * 2D visualizer using JFreeChart library
 */
public class TwoDVisualizerModule implements MZmineRunnableModule {

  private static final String MODULE_NAME = "2D plot";
  private static final String MODULE_DESCRIPTION = "2D plot shows retention time, m/z, intensity as plot.";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  public static void show2DVisualizerSetupDialog(RawDataFile dataFile, Range<Double> mzRange,
      Range<Float> rtRange) {

    ParameterSet parameters = MZmineCore.getConfiguration()
        .getModuleParameters(TwoDVisualizerModule.class);

    parameters.getParameter(TwoDVisualizerParameters.dataFiles)
        .setValue(RawDataFilesSelectionType.SPECIFIC_FILES, new RawDataFile[]{dataFile});

    if (rtRange != null) {
      parameters.getParameter(TwoDVisualizerParameters.scanSelection)
          .setValue(new ScanSelection(1, rtRange));
    }
    if (mzRange != null) {
      parameters.getParameter(TwoDVisualizerParameters.mzRange).setValue(mzRange);
    }

    ExitCode exitCode = parameters.showSetupDialog(true);

    if (exitCode != ExitCode.OK) {
      return;
    }

    ScanSelection scanSel = parameters.getParameter(TwoDVisualizerParameters.scanSelection)
        .getValue();
    Scan[] scans = scanSel.getMatchingScans(dataFile);
    rtRange = ScanUtils.findRtRange(scans);

    mzRange = parameters.getParameter(TwoDVisualizerParameters.mzRange).getValue();

    TwoDVisualizerTab newWindow = new TwoDVisualizerTab(dataFile, scans, rtRange, mzRange,
        parameters);

    //newWindow.show();
    MZmineCore.getDesktop().addTab(newWindow);
  }

  public static void show2DVisualizerSetupDialog(RawDataFile dataFile) {
    show2DVisualizerSetupDialog(dataFile, null, null);
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    RawDataFile[] dataFiles = parameters.getParameter(TwoDVisualizerParameters.dataFiles).getValue()
        .getMatchingRawDataFiles();
    ScanSelection scanSel = parameters.getParameter(TwoDVisualizerParameters.scanSelection)
        .getValue();
    Scan[] scans = scanSel.getMatchingScans(dataFiles[0]);
    Range<Float> rtRange = ScanUtils.findRtRange(scans);

    Range<Double> mzRange = parameters.getParameter(TwoDVisualizerParameters.mzRange).getValue();
    TwoDVisualizerTab newTab = new TwoDVisualizerTab(dataFiles[0], scans, rtRange, mzRange,
        parameters);

    //newWindow.show();
    MZmineCore.getDesktop().addTab(newTab);

    return ExitCode.OK;
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
