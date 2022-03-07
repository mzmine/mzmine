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

package io.github.mzmine.modules.io.import_rawdata_zip;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Raw data import module
 */
public class ZipImportModule implements MZmineProcessingModule {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final String MODULE_NAME = "ZIP/gZIP file import";
  private static final String MODULE_DESCRIPTION = "This module imports raw data into the project.";

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
    return MZmineModuleCategory.RAWDATAIMPORT;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return ZipImportParameters.class;
  }

  @Override
  @NotNull
  public ExitCode runModule(final @NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {

    // one storage per module call
    final MemoryMapStorage storage = MemoryMapStorage.forRawDataFile();
    File fileNames[] = parameters.getParameter(ZipImportParameters.fileNames).getValue();

    if (Arrays.asList(fileNames).contains(null)) {
      logger.warning("List of filenames contains null");
      return ExitCode.ERROR;
    }

    for (int i = 0; i < fileNames.length; i++) {
      if (fileNames[i] == null) {
        return ExitCode.OK;
      }

      if ((!fileNames[i].exists()) || (!fileNames[i].canRead())) {
        MZmineCore.getDesktop().displayErrorMessage("Cannot read file " + fileNames[i]);
        logger.warning("Cannot read file " + fileNames[i]);
        return ExitCode.ERROR;
      }

      Task newTask = new ZipImportTask(project, fileNames[i], ZipImportModule.class,
          parameters, moduleCallDate, storage);
      tasks.add(newTask);
    }

    return ExitCode.OK;
  }

}
