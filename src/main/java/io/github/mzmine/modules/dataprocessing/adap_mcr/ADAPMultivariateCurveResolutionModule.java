/*
 * Copyright (C) 2016 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */
package io.github.mzmine.modules.dataprocessing.adap_mcr;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

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
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    Map<RawDataFile, ChromatogramPeakPair> lists =
        ChromatogramPeakPair.fromParameterSet(parameters);

    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();

    for (Map.Entry<RawDataFile, ChromatogramPeakPair> e : lists.entrySet()) {
      Task newTask = new ADAP3DecompositionV2Task(project, e.getValue(), e.getKey(), parameters, storage, moduleCallDate);
      tasks.add(newTask);
    }

    return ExitCode.OK;
  }
}
