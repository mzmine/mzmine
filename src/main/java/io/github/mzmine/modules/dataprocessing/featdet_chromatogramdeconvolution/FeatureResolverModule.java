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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
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

public abstract class FeatureResolverModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Chromatogram deconvolution";
  private static final String MODULE_DESCRIPTION = "This module separates each detected chromatogram into individual peaks.";

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATURE_RESOLVING;
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull final ParameterSet parameters,
      @NotNull final Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    // one memory map storage per module call to reduce number of files and connect related feature lists
    MemoryMapStorage storage = MemoryMapStorage.forFeatureList();

    FeatureList[] peakLists = parameters.getParameter(GeneralResolverParameters.PEAK_LISTS)
        .getValue().getMatchingFeatureLists();
    for (final FeatureList peakList : peakLists) {
      tasks.add(new FeatureResolverTask(project, storage, peakList, parameters,
          FeatureDataUtils.DEFAULT_CENTER_FUNCTION, moduleCallDate));
    }

    return ExitCode.OK;
  }
}
