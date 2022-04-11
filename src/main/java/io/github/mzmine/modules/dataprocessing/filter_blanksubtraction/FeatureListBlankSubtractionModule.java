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

package io.github.mzmine.modules.dataprocessing.filter_blanksubtraction;

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

/**
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class FeatureListBlankSubtractionModule implements MZmineProcessingModule {

  public static final String MODULE_NAME = "Feature list blank subtraction";

  @Override
  public String getName() {
    return MODULE_NAME;
  }

  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return FeatureListBlankSubtractionParameters.class;
  }

  @Override
  public String getDescription() {
    return "Subtracts features appearing in (procedural) blank measurements feature list from an aligned feature list.";
  }

  @Override
  public ExitCode runModule(MZmineProject project, ParameterSet parameters, Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {

    Task task = new FeatureListBlankSubtractionTask(project,
        (FeatureListBlankSubtractionParameters) parameters, MemoryMapStorage.forFeatureList(), moduleCallDate);

    tasks.add(task);

    return ExitCode.OK;
  }

  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATURELISTFILTERING;
  }

}
