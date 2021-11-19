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

package io.github.mzmine.modules.tools.qualityparameters;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QualityParametersModule implements MZmineRunnableModule {

  public static final String DESCRIPTION = "Calculates quality parameters such as FWHM, asymmetry factor, tailing factor, S/N ratio.";
  public static final String NAME = "Quality parameters";

  @NotNull
  @Override
  public String getDescription() {
    return DESCRIPTION;
  }

  @NotNull
  @Override
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    for (FeatureList featureList : parameters.getParameter(QualityParametersParameters.peakLists)
        .getValue().getMatchingFeatureLists()) {
      runModule(featureList, parameters, moduleCallDate);
    }
    return ExitCode.OK;
  }

  public ExitCode runModule(FeatureList[] featureLists, ParameterSet parameters, @NotNull Instant moduleCallDate) {
    for(FeatureList featureList : featureLists){
      runModule(featureList, parameters, moduleCallDate);
    }
    return ExitCode.OK;
  }

  public ExitCode runModule(FeatureList featureList, ParameterSet parameters, @NotNull Instant moduleCallDate) {
    MZmineCore.getTaskController().addTask(new QualityParametersTask(featureList, parameters, moduleCallDate));
    return ExitCode.OK;
  }

  @NotNull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.TOOLS;
  }

  @NotNull
  @Override
  public String getName() {
    return NAME;
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return QualityParametersParameters.class;
  }
}
