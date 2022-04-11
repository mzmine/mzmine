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

package io.github.mzmine.modules.example;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

/**
 * A Module creates tasks which are then added queue
 */
public class LearnerModule implements MZmineProcessingModule {
  // #################################################################
  // IMPORTANT
  // do not forget to put your module in the BatchModeModulesList
  // in the package: io.github.mzmine.main
  // #################################################################

  private static final String MODULE_NAME = "Learner module";
  private static final String MODULE_DESCRIPTION = "This module is for learners only";

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {

    // get parameters
    FeatureList featureLists[] =
        parameters.getParameter(LearnerParameters.featureLists).getValue().getMatchingFeatureLists();
    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();

    // create and start one task for each feature list
    for (final FeatureList featureList : featureLists) {
      Task newTask = new FeatureListRowLearnerTask(project, featureList, parameters, storage, moduleCallDate);
      tasks.add(newTask);
    }

    return ExitCode.OK;

  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    /**
     * Change category: will automatically be added to the linked menu
     */
    return MZmineModuleCategory.FEATURELISTFILTERING;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return LearnerParameters.class;
  }

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }
}
