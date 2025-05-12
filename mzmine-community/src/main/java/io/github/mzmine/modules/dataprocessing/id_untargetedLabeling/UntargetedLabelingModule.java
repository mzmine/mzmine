/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_untargetedLabeling;

import static io.github.mzmine.gui.mainwindow.MZmineTab.logger;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/**
 * This module implements untargeted isotope labeling analysis based on feature lists from unlabeled
 * samples.
 */
public class UntargetedLabelingModule implements MZmineProcessingModule {

  public static final String MODULE_NAME = "Untargeted isotope labeling (feature-based)";
  public static final String MODULE_DESCRIPTION =
      "This module detects isotopically labeled compounds by identifying features "
          + "in unlabeled samples and searching for corresponding labeled patterns in labeled samples.";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ISOTOPES;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return UntargetedLabelingParameters.class;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {

    // Get unlabeled feature lists
    FeatureList[] unlabeledFeatureLists = parameters.getParameter(
        UntargetedLabelingParameters.unlabeledFeatureLists).getValue().getMatchingFeatureLists();

    // Get labeled feature lists
    FeatureList[] labeledFeatureLists = parameters.getParameter(
        UntargetedLabelingParameters.labeledFeatureLists).getValue().getMatchingFeatureLists();

    // Ensure we have both labeled and unlabeled feature lists
    if (unlabeledFeatureLists.length == 0 || labeledFeatureLists.length == 0) {
      logger.warning("Either unlabeled or labeled feature lists are missing");
      return ExitCode.ERROR;
    }

    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();

    // For each unlabeled feature list, pair with each labeled feature list
    for (FeatureList unlabeledFeatureList : unlabeledFeatureLists) {
      for (FeatureList labeledFeatureList : labeledFeatureLists) {
        Task newTask = new UntargetedLabelingTask(project, unlabeledFeatureList, labeledFeatureList,
            parameters, storage, moduleCallDate);
        tasks.add(newTask);
      }
    }

    return ExitCode.OK;
  }
}