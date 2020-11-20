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

package io.github.mzmine.modules.dataprocessing.norm_linear;

import io.github.mzmine.datamodel.data.FeatureList;
import java.util.Collection;

import javax.annotation.Nonnull;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

public class LinearNormalizerModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Linear normalizer";
  private static final String MODULE_DESCRIPTION =
      "Linear normalizer divides the height (or area) of each feature in the feature list by a normalization factor, determined according to the given normalization type.";

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

    FeatureList featureLists[] = parameters.getParameter(LinearNormalizerParameters.featureLists).getValue()
        .getMatchingPeakLists();

    for (FeatureList featureList : featureLists) {
      Task newTask = new LinearNormalizerTask(project, featureList, parameters);
      tasks.add(newTask);
    }

    return ExitCode.OK;

  }

  @Override
  public @Nonnull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.NORMALIZATION;
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return LinearNormalizerParameters.class;
  }

}
