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

package io.github.mzmine.modules.visualization.projectmetadata;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.io.deprecated_jmzml.MzMLImportParameters;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectMetadataImportModule implements MZmineProcessingModule {

  private static final Logger logger = Logger.getLogger(
      ProjectMetadataImportModule.class.getName());

  private static final String MODULE_NAME = "Project metadata import";
  private static final String MODULE_DESCRIPTION = "This module imports metadata into the project from .tsv-format files.";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return ProjectMetadataImportParameters.class;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.PROJECTMETADATA;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {
    // get the all selected files
    File[] fileNames = parameters.getParameter(ProjectMetadataImportParameters.fileNames).getValue();

    // null check
    if (Arrays.asList(fileNames).contains(null)) {
      logger.warning("List of filenames contains null");
      return ExitCode.ERROR;
    }

    try {
      tasks.add(new ProjectMetadataImportTask(fileNames, moduleCallDate));
    } catch (Exception e) {
      logger.severe(e.getMessage());
      return ExitCode.ERROR;
    }

    return ExitCode.OK;
  }
}
