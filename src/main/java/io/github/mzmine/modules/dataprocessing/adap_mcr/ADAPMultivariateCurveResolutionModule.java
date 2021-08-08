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
package io.github.mzmine.modules.dataprocessing.adap_mcr;

import io.github.mzmine.util.MemoryMapStorage;
import java.util.Collection;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAPMultivariateCurveResolutionModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Multivariate Curve Resolution";
  private static final String MODULE_DESCRIPTION = "This method "
      + "combines peaks into analytes and constructs fragmentation spectrum for each analyte";

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
    return MZmineModuleCategory.SPECTRALDECONVOLUTION;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return ADAP3DecompositionV2Parameters.class;
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks) {
    Map<RawDataFile, ChromatogramPeakPair> lists =
        ChromatogramPeakPair.fromParameterSet(parameters);

    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();

    for (ChromatogramPeakPair pair : lists.values()) {
      Task newTask = new ADAP3DecompositionV2Task(project, pair, parameters, storage);
      tasks.add(newTask);
    }

    return ExitCode.OK;
  }
}
