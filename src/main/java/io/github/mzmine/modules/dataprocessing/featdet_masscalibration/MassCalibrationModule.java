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

package io.github.mzmine.modules.dataprocessing.featdet_masscalibration;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

import io.github.mzmine.util.MemoryMapStorage;
import javax.annotation.Nonnull;
import java.util.Collection;

public class MassCalibrationModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Mass calibration";
  private static final String MODULE_DESCRIPTION =
      "This module estimates systematic error of measurement by analysing differences between actual mass peaks"
          + " and masses of assigned compounds, it then shifts the mass peaks to account against that bias.";

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

    // create a single storage map for all mass lists that were created with the same parameters
    final MemoryMapStorage storageMemoryMap = new MemoryMapStorage();

    RawDataFile[] dataFiles = parameters.getParameter(MassCalibrationParameters.dataFiles)
        .getValue().getMatchingRawDataFiles();
    for (RawDataFile dataFile : dataFiles) {
      Task newTask = new MassCalibrationTask(dataFile, parameters.cloneParameterSet(), storageMemoryMap);
      tasks.add(newTask);
    }

    return ExitCode.OK;
  }

  @Override
  public @Nonnull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATUREDETECTION;
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return MassCalibrationParameters.class;
  }

}
