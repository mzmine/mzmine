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

package io.github.mzmine.modules.dataprocessing.id_ms2search;

import io.github.mzmine.datamodel.features.FeatureList;
import java.time.Instant;
import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

public class Ms2SearchModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "MS2 similarity search";
  private static final String MODULE_DESCRIPTION =
      "This method searches for similar MS2 fragmentation spectra between two feature lists";

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

    FeatureList peakList1[] =
        parameters.getParameter(Ms2SearchParameters.peakList1).getValue().getMatchingFeatureLists();

    FeatureList peakList2[] =
        parameters.getParameter(Ms2SearchParameters.peakList2).getValue().getMatchingFeatureLists();

    // Previously iterated over all the peaklists & did a separate task for
    // each.
    // Now a single task.
    Task newTask = new Ms2SearchTask(parameters, peakList1[0], peakList2[0], moduleCallDate);
    tasks.add(newTask);

    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ANNOTATION;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return Ms2SearchParameters.class;
  }

}
