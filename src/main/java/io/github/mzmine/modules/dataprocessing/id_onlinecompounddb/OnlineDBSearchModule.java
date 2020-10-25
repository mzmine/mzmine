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

package io.github.mzmine.modules.dataprocessing.id_onlinecompounddb;

import io.github.mzmine.datamodel.data.FeatureList;
import io.github.mzmine.datamodel.data.FeatureListRow;
import java.util.Collection;
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import javafx.application.Platform;

/**
 * Module for identifying peaks by searching on-line databases.
 */
public class OnlineDBSearchModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Online database search";
  private static final String MODULE_DESCRIPTION =
      "This module attepts to find those peaks in a feature list, which form an isotope pattern.";

  @Override
  public @Nonnull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @Nonnull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @Nonnull
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    final FeatureList[] featureLists = parameters.getParameter(PeakListIdentificationParameters.peakLists)
        .getValue().getMatchingPeakLists();
    for (final FeatureList featureList : featureLists) {
      Task newTask = new PeakListIdentificationTask(parameters, featureList);
      tasks.add(newTask);
    }

    return ExitCode.OK;
  }

  /**
   * Show dialog for identifying a single peak-list row.
   *
   * @param row the feature list row.
   */
  public static void showSingleRowIdentificationDialog(final FeatureListRow row) {

    assert Platform.isFxApplicationThread();

    final ParameterSet parameters = new SingleRowIdentificationParameters();

    // Set m/z.
    parameters.getParameter(SingleRowIdentificationParameters.NEUTRAL_MASS)
        .setIonMass(row.getAverageMZ());

    // Set charge.
    final int charge = row.getBestPeak().getCharge();
    if (charge > 0) {

      parameters.getParameter(SingleRowIdentificationParameters.NEUTRAL_MASS).setCharge(charge);
    }

    // Run task.
    if (parameters.showSetupDialog(true) == ExitCode.OK) {

      MZmineCore.getTaskController()
          .addTask(new SingleRowIdentificationTask(parameters.cloneParameterSet(), row));
    }
  }

  @Override
  public @Nonnull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.IDENTIFICATION;
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return PeakListIdentificationParameters.class;
  }
}
