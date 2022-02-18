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

package io.github.mzmine.modules.dataprocessing.id_sirius;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import javafx.application.Platform;

public class SiriusIdentificationModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "SIRIUS structure prediction";
  private static final String MODULE_DESCRIPTION = "Sirius identification method.";

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

    final FeatureList[] peakLists = parameters.getParameter(PeakListIdentificationParameters.peakLists)
        .getValue().getMatchingFeatureLists();
    for (final FeatureList peakList : peakLists) {
      Task newTask = new PeakListIdentificationTask(parameters, peakList, moduleCallDate);
      tasks.add(newTask);
    }

    return ExitCode.OK;
  }

  /**
   * Show dialog for identifying a single peak-list row.
   *
   * @param row the feature list row.
   */
  public static void showSingleRowIdentificationDialog(final FeatureListRow row, @NotNull Instant moduleCallDate) {

    assert Platform.isFxApplicationThread();

    final ParameterSet parameters = new SingleRowIdentificationParameters();

    // Set m/z.
    parameters.getParameter(SingleRowIdentificationParameters.ION_MASS)
        .setValue(row.getAverageMZ());

    if (parameters.showSetupDialog(true) == ExitCode.OK) {
      int fingerCandidates, siriusCandidates, timer;
      timer = parameters.getParameter(SingleRowIdentificationParameters.SIRIUS_TIMEOUT).getValue();
      siriusCandidates =
          parameters.getParameter(SingleRowIdentificationParameters.SIRIUS_CANDIDATES).getValue();
      fingerCandidates =
          parameters.getParameter(SingleRowIdentificationParameters.FINGERID_CANDIDATES).getValue();

      if (timer <= 0 || siriusCandidates <= 0 || fingerCandidates <= 0) {
        MZmineCore.getDesktop().displayErrorMessage("Sirius parameters can't be negative");
      }
      else { // Run task.
        MZmineCore.getTaskController()
            .addTask(new SingleRowIdentificationTask(parameters.cloneParameterSet(), row, moduleCallDate));
      }
    }
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ANNOTATION;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return PeakListIdentificationParameters.class;
  }
}
