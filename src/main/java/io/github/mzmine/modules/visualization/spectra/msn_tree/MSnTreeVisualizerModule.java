/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.spectra.msn_tree;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MSnTreeVisualizerModule implements MZmineModule {

  public static final String MODULE_NAME = "MS(n) spectra tree";

  public static void showNewTab() {
    RawDataFile[] raw = MZmineCore.getDesktop().getSelectedDataFiles();
    MZmineCore.runLater(() -> {
      MSnTreeTab tab = new MSnTreeTab();
      MZmineCore.getDesktop().addTab(tab);
      if (raw != null && raw.length > 0) {
        tab.setRawDataFile(raw[0]);
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
