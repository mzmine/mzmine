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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking;


import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class IonNetworkingModule implements MZmineProcessingModule {

  private static final String NAME = "Ion identity networking";

  private static final String DESCRIPTION =
      "This method searches for adducts, in-source fragments, and clusters";

  @NotNull
  @Override
  public String getName() {
    return NAME;
  }

  @NotNull
  @Override
  public String getDescription() {
    return DESCRIPTION;
  }

  @NotNull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ION_IDENTITY_NETWORKS;
  }

  @NotNull
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return IonNetworkingParameters.class;
  }

  @NotNull
  @Override
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull final ParameterSet parameters,
      @NotNull final Collection<Task> tasks, @NotNull Instant moduleCallDate) {

    ModularFeatureList[] pkl = parameters.getParameter(IonNetworkingParameters.PEAK_LISTS)
        .getValue()
        .getMatchingFeatureLists();
    for (ModularFeatureList p : pkl) {
      tasks.add(new IonNetworkingTask(project, parameters, p, moduleCallDate));
    }

    return ExitCode.OK;
  }
}
