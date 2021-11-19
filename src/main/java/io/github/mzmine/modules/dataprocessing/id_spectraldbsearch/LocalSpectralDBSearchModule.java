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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.dataprocessing.id_spectraldbsearch;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import java.util.Date;
import org.jetbrains.annotations.NotNull;

public class LocalSpectralDBSearchModule implements MZmineProcessingModule {

  public static final String MODULE_NAME = "Local spectra database search";
  private static final String MODULE_DESCRIPTION =
      "This method searches all feature list rows (from all feature lists) against a local spectral database (needs to be loaded first).";

  /**
   * Show dialog for identifying multiple selected peak-list rows.
   *
   * @param rows the feature list row.
   */
  public static void showSelectedRowsIdentificationDialog(final FeatureListRow[] rows,
      FeatureTableFX table, @NotNull Date moduleCallDate) {

    final ParameterSet parameters = new SelectedRowsLocalSpectralDBSearchParameters();

    if (parameters.showSetupDialog(true) == ExitCode.OK) {

      MZmineCore.getTaskController().addTask(
          new SelectedRowsLocalSpectralDBSearchTask(rows, table, parameters.cloneParameterSet(),
              moduleCallDate));
    }
  }

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
      @NotNull Collection<Task> tasks, @NotNull Date moduleCallDate) {
    final ModularFeatureList[] featureLists = parameters
        .getParameter(LocalSpectralDBSearchParameters.peakLists)
        .getValue().getMatchingFeatureLists();
    Task newTask = new LocalSpectralDBSearchTask(parameters, featureLists, moduleCallDate);
    tasks.add(newTask);

    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.IDENTIFICATION;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return LocalSpectralDBSearchParameters.class;
  }

}
