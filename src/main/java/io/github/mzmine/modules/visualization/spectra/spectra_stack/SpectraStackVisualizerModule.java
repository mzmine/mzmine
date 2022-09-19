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

package io.github.mzmine.modules.visualization.spectra.spectra_stack;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Spectrum visualizer
 */
public class SpectraStackVisualizerModule implements MZmineModule {

  private static final String MODULE_NAME = "Spectra stack visualizer";

  public static void addMsMsStackVisualizer(List<ModularFeatureListRow> rows,
      Collection<RawDataFile> rawDataFiles, RawDataFile selectedFile) {

    SpectraStackVisualizerPane content = new SpectraStackVisualizerPane();
    content.setData(rows.toArray(new FeatureListRow[0]), rawDataFiles.toArray(new RawDataFile[0]),
        selectedFile, true, SortingProperty.MZ, SortingDirection.Ascending);
    SimpleTab tab = new SimpleTab("Multiple MS/MS (" + rows.size() + ")", content);
    MZmineCore.getDesktop().addTab(tab);
  }

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return SpectraStackVisualizerParameters.class;
  }

}
