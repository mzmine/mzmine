/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.spectra.msn_tree;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.gui.mainwindow.ProjectTab;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MSnTreeVisualizerModule implements MZmineRunnableModule {

  public static final String MODULE_NAME = "MSn spectral trees";

  public static final String DESCRIPTION = "Open fragment spectra trees of MS2 to MSn.";

  public static void showNewTab() {
    if (!(MZmineCore.getDesktop() instanceof MZmineGUI desktop)) {
      throw new IllegalStateException("Cannot open msn trees in CLI");
    }

    ProjectTab selectedTab = desktop.getSelectedProjectTab();

    FxThread.runLater(() -> {
      String errorMessage = null;
      switch (selectedTab) {
        case LIBRARIES -> errorMessage = "data file or a feature list";
        case DATA_FILES -> {
          List<RawDataFile> raws = MZmineGUI.getSelectedRawDataFiles();
          if (raws.isEmpty()) {
            errorMessage = "data file";
          }
          MSnTreeTab tab = new MSnTreeTab();
          desktop.addTab(tab);
          tab.setRawDataFile(raws.getFirst());
        }
        case FEATURE_LISTS -> {
          List<FeatureList> featureLists = MZmineGUI.getSelectedFeatureLists();
          if (featureLists.isEmpty()) {
            errorMessage = "feature list";
          }
          MSnTreeTab tab = new MSnTreeTab();
          desktop.addTab(tab);
          tab.setFeatureList(featureLists.getFirst());
        }
      }

      if (errorMessage != null) {
        DialogLoggerUtil.showMessageDialogForTime("Selection needed",
            "Select a %s in the main window to open an MSn tree".formatted(errorMessage), 5000);
      }
    });
  }

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return MSnTreeVisualizerParameters.class;
  }

  @Override
  public @NotNull String getDescription() {
    return DESCRIPTION;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull final MZmineProject project,
      @NotNull final ParameterSet parameters, @NotNull final Collection<Task> tasks,
      @NotNull final Instant moduleCallDate) {
    showNewTab();
    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATION_RAW_AND_FEATURE;
  }
}
