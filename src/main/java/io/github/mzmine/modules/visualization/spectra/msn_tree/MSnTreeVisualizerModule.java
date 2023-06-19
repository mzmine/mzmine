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

package io.github.mzmine.modules.visualization.spectra.msn_tree;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.DialogLoggerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MSnTreeVisualizerModule implements MZmineModule {

  public static final String MODULE_NAME = "MS(n) spectra tree";

  public static void showNewTab() {
    RawDataFile[] raw = MZmineCore.getDesktop().getSelectedDataFiles();
    FeatureList[] flists = MZmineCore.getDesktop().getSelectedPeakLists();
    if ((raw == null || raw.length == 0) && (flists == null || flists.length == 0)) {
      DialogLoggerUtil.showMessageDialogForTime("Selection needed",
          "Select a data file or feature to open the MSn tree", 5000);
      return;
    }

    MZmineCore.runLater(() -> {
      MSnTreeTab tab = new MSnTreeTab();
      MZmineCore.getDesktop().addTab(tab);
      if (raw != null && raw.length > 0) {
        tab.setRawDataFile(raw[0]);
      }
      if (flists != null && flists.length > 0) {
        tab.setFeatureList(flists[0]);
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
}
