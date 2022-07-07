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

package io.github.mzmine.modules.dataprocessing.id_ccscalc;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Calculates feature specific collision cross section values based on ion mobility values and the
 * charge of a feature. For calculation, a mobility value {@link io.github.mzmine.datamodel.features.types.numbers.MobilityType},
 * the unit of that mobility value {@link io.github.mzmine.datamodel.MobilityType} and the charge
 * {@link io.github.mzmine.datamodel.features.types.numbers.ChargeType} of an ion are necessary.
 *
 * @author https://github.com/SteffenHeu
 * @see CCSCalcTask
 * @see io.github.mzmine.datamodel.features.types.numbers.CCSType
 * @see https://en.wikipedia.org/wiki/Cross_section_(physics)#Collision_among_gas_particles
 * @see https://doi.org/10.1002/jssc.201700919
 * @see https://arxiv.org/ftp/arxiv/papers/1709/1709.02953.pdf
 * @see https://www.waters.com/webassets/cms/library/docs/720005374en.pdf or
 * https://www.sciencedirect.com/science/article/abs/pii/S1367593117301229?via%3Dihub
 */
public class CCSCalcModule implements MZmineProcessingModule {

  public static final String NAME = "CCS calculation module";

  @NotNull
  @Override
  public String getName() {
    return NAME;
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return CCSCalcParameters.class;
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Calculates CCS values for features.";
  }

  @NotNull
  @Override
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    Task task = new CCSCalcTask(project, parameters, MemoryMapStorage.forFeatureList(), moduleCallDate);
    tasks.add(task);
    return ExitCode.OK;
  }

  @NotNull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ANNOTATION;
  }
}
