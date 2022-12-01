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

package io.github.mzmine.modules.dataprocessing.id_onlinecompounddb;

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

/**
 * Module for identifying peaks by searching on-line databases.
 */
public class OnlineDBSearchModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Online database search";
  private static final String MODULE_DESCRIPTION = "This module attempts to find those peaks in a feature list, which form an isotope pattern.";

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

    final FeatureList[] featureLists = parameters
        .getParameter(PeakListIdentificationParameters.peakLists).getValue()
        .getMatchingFeatureLists();
    for (final FeatureList featureList : featureLists) {
      Task newTask = new PeakListIdentificationTask(parameters, featureList, moduleCallDate);
      tasks.add(newTask);
    }

    return ExitCode.OK;
  }

  /**
   * Show dialog for identifying a single peak-list row.
   *
   * @param row the feature list row.
   */
  public static void showSingleRowIdentificationDialog(final FeatureListRow row,
      @NotNull Instant moduleCallDate) {

    assert Platform.isFxApplicationThread();

    final ParameterSet parameters = new SingleRowIdentificationParameters();

    // Set m/z.
    parameters.getParameter(SingleRowIdentificationParameters.NEUTRAL_MASS)
        .setIonMass(row.getAverageMZ());

    // Set charge.
    final int charge = row.getBestFeature().getCharge();
    if (charge > 0) {

      parameters.getParameter(SingleRowIdentificationParameters.NEUTRAL_MASS).setCharge(charge);
    }

    // Run task.
    if (parameters.showSetupDialog(true) == ExitCode.OK) {

      MZmineCore.getTaskController().addTask(
          new SingleRowIdentificationTask(parameters.cloneParameterSet(), row, moduleCallDate));
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
