/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.addionannotations;


import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

import javax.annotation.Nonnull;
import java.util.Collection;

public class AddIonNetworkingModule implements MZmineProcessingModule {

  private static final String NAME = "Add ion identities to networks";

  private static final String DESCRIPTION = "This module adds ion identities to existing networks";

   @Nonnull
   @Override
   public String getName() {
    return NAME;
  }

  @Override
  public @Nonnull String getDescription() {
    return DESCRIPTION;
  }

  @Override
  public @Nonnull
  MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ION_IDENTITY_NETWORKS;
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return AddIonNetworkingParameters.class;
  }

  @Nonnull
  @Override
  public ExitCode runModule(@Nonnull MZmineProject project,
                            @Nonnull ParameterSet parameters,
                            @Nonnull Collection<Task> tasks) {
    ModularFeatureList[] pkl = parameters.getParameter(AddIonNetworkingParameters.PEAK_LISTS).getValue()
        .getMatchingFeatureLists();
    for (ModularFeatureList p : pkl)
      tasks.add(new AddIonNetworkingTask(project, parameters, p));

    return ExitCode.OK;
  }
}
