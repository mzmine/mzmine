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

package io.github.mzmine.modules.visualization.rawdataoverviewims;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IMSRawDataOverviewModule implements MZmineRunnableModule {

  public static void openIMSVisualizerTabWithFeatures(List<ModularFeature> features) {
    if (features.isEmpty() || features.get(0) == null) {
      return;
    }

    final List<MZmineTab> tabs = MZmineCore.getDesktop().getAllTabs();
    IMSRawDataOverviewTab tab = null;
    for (MZmineTab t : tabs) {
      if (t instanceof IMSRawDataOverviewTab) {
        tab = (IMSRawDataOverviewTab) t;
        break;
      }
    }

    final ModularFeature feature = features.get(0);

    // if no tab was found, make a new one.
    if (tab == null) {
      final RawDataFilesSelection rawFileSelection = new RawDataFilesSelection(
          RawDataFilesSelectionType.SPECIFIC_FILES);
      rawFileSelection.setSpecificFiles(new RawDataFile[]{feature.getRawDataFile()});
      final MZTolerance tolerance = MZTolerance
          .getMaximumDataPointTolerance((List<Feature>) (List<? extends Feature>) features);

      final ParameterSet parameterSet = MZmineCore.getConfiguration()
          .getModuleParameters(IMSRawDataOverviewModule.class).cloneParameterSet();
      parameterSet.getParameter(IMSRawDataOverviewParameters.rawDataFiles)
          .setValue(rawFileSelection);
      parameterSet.getParameter(IMSRawDataOverviewParameters.mzTolerance).setValue(tolerance);

      tab = new IMSRawDataOverviewTab(parameterSet);
    }

    final var finalTab = tab;
    MZmineCore.runLater(() -> {
      MZmineCore.getDesktop().addTab(finalTab);
      finalTab.onRawDataFileSelectionChanged(List.of(feature.getRawDataFile()));
      IMSRawDataOverviewPane pane = (IMSRawDataOverviewPane) finalTab.getContent();
      pane.setSelectedFrame((Frame) feature.getRepresentativeScan());
      pane.addRanges(
          features.stream().map(f -> f.getRawDataPointsMZRange()).collect(Collectors.toList()));
    });
  }

  @NotNull
  @Override
  public String getName() {
    return "Ion mobility raw data overview";
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return IMSRawDataOverviewParameters.class;
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Visualizes ion mobility raw data files.";
  }

  @NotNull
  @Override
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    RawDataFilesParameter param = parameters
        .getParameter(IMSRawDataOverviewParameters.rawDataFiles);
    RawDataFilesSelection selection = param.getValue();
    RawDataFile[] files = selection.getMatchingRawDataFiles();
    for (RawDataFile file : files) {
      if (!(file instanceof IMSRawDataFile)) {
        MZmineCore.getDesktop().displayMessage("Invalid file type",
            "Cannot display raw data file " + file.getName() + " in \"" + getName()
                + "\", since it is does not possess an ion mobility dimension.");
        continue;
      }
      MZmineTab tab = new IMSRawDataOverviewTab(parameters);
      tab.onRawDataFileSelectionChanged(Set.of(file));
      MZmineCore.getDesktop().addTab(tab);
    }

    return ExitCode.OK;
  }

  @NotNull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONRAWDATA;
  }
}
