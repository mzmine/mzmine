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

package io.github.mzmine.modules.io.import_features_mztabm;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.Collection;

public class MZTabmImportModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Import mzTab-m file";
  private static final String MODULE_DESCRIPTION =
      "This method imports a feature list from a mzTab-m file.";

  @NotNull
  @Override
  public String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @NotNull
  @Override
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    File inputFiles[] = parameters.getParameter(MzTabmImportParameters.file).getValue();
    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();
    for (File inputFile : inputFiles) {
      MzTabmImportTask task = new MzTabmImportTask(project, parameters, inputFile, storage, moduleCallDate);
      tasks.add(task);
    }
    return ExitCode.OK;
  }

  @NotNull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATURELISTIMPORT;
  }

  @NotNull
  @Override
  public String getName() {
    return MODULE_NAME;
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return MzTabmImportParameters.class;
  }
}
