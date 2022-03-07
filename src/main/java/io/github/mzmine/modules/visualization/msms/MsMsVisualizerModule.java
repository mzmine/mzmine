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

package io.github.mzmine.modules.visualization.msms;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class MsMsVisualizerModule implements MZmineRunnableModule {

  private static final String MODULE_NAME = "MS/MS data visualizer";
  private static final String MODULE_DESCRIPTION = "Scatter plot for MS/MS data visualization, four"
      + "axes types supported: retention time, precursor m/z, product m/z, neutral loss";

  @Override
  @NotNull
  public String getName() {
    return MODULE_NAME;
  }

  @Override
  @NotNull
  public String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    assert MZmineCore.getDesktop() != null;

    MsMsVisualizerTab newTab = new MsMsVisualizerTab(parameters);
    MZmineCore.getDesktop().addTab(newTab);

    return ExitCode.OK;
  }

  @Override
  public @NotNull
  MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONRAWDATA;
  }

  @Override
  public @NotNull
  Class<? extends ParameterSet> getParameterSetClass() {
    return MsMsParameters.class;
  }

}
