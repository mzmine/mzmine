/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.formula.createavgformulas;


import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

import javax.annotation.Nonnull;
import java.util.Collection;

public class CreateAvgNetworkFormulasModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Average formulas for annotation networks";
  private static final String MODULE_DESCRIPTION =
      "Creates a list of average molecular formulas from all formula lists of all ions in this network (e.g., M+H and M+Na)";

  @Override
  public @Nonnull
  String getName() {
    return MODULE_NAME;
  }

  @Override
  public @Nonnull
  MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ION_IDENTITY_NETWORKS;
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return CreateAvgNetworkFormulasParameters.class;
  }

  @Override
  public @Nonnull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  public @Nonnull
  ExitCode runModule(@Nonnull MZmineProject project,
                     @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks) {
    FeatureList featureLists[] = parameters.getParameter(CreateAvgNetworkFormulasParameters.PEAK_LISTS)
        .getValue().getMatchingFeatureLists();

    for (FeatureList featureList : featureLists) {
      Task newTask = new CreateAvgNetworkFormulasTask(featureList, parameters);
      tasks.add(newTask);
    }
    return ExitCode.OK;
  }
}
