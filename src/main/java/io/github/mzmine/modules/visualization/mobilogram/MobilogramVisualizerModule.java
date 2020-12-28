/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.mobilogram;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.util.Arrays;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MobilogramVisualizerModule implements MZmineRunnableModule {

  @Nonnull
  @Override
  public String getDescription() {
    return "Visualizes mobilograms of ion mobility frames";
  }

  @Nonnull
  @Override
  public ExitCode runModule(@Nonnull MZmineProject project,
      @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    MobilogramVisualizerTab tab = new MobilogramVisualizerTab();
    RawDataFile[] files = parameters.getParameter(MobilogramVisualizerParameters.rawFiles)
        .getValue().getMatchingRawDataFiles();
    MZmineCore.getDesktop().addTab(tab);
    tab.onRawDataFileSelectionChanged(Arrays.asList(files));

    return ExitCode.OK;
  }

  @Nonnull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONRAWDATA;
  }

  @Nonnull
  @Override
  public String getName() {
    return "Mobilogram visualizer";
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return MobilogramVisualizerParameters.class;
  }
}
