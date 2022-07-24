/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package io.github.mzmine.modules.io.import_waters;

import com.google.common.base.Strings;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFImportParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RawDataFileUtils;
import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WatersImportModule implements MZmineProcessingModule {
  private static final String MODULE_NAME = "Water Raw file import";
  private static final String MODULE_DESCRIPTION = "This module imports raw data into the project.";
  private static final Logger logger = Logger.getLogger(WatersImportModule.class.getName());


  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return WatersImportParameters.class;}

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }


  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.RAWDATAIMPORT;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate)
  {

    File fileNames[] = parameters.getParameter(WatersImportParameters.fileNames).getValue();

    if (Arrays.asList(fileNames).contains(null)) {
      logger.warning("List of filenames contains null");
      return ExitCode.ERROR;
    }

    // Find common prefix in raw file names if in GUI mode
    String commonPrefix = RawDataFileUtils.askToRemoveCommonPrefix(fileNames);

    for (int i = 0; i < fileNames.length; i++) {

      if ((!fileNames[i].exists()) || (!fileNames[i].canRead())) {
        MZmineCore.getDesktop().displayErrorMessage("Cannot read file " + fileNames[i]);
        logger.warning("Cannot read file " + fileNames[i]);
        return ExitCode.ERROR;
      }

      // Set the new name by removing the common prefix
      String newName;
      if (!Strings.isNullOrEmpty(commonPrefix)) {
        final String regex = "^" + Pattern.quote(commonPrefix);
        newName = fileNames[i].getName().replaceFirst(regex, "");
      } else {
        newName = fileNames[i].getName();
      }

      try {
        // IMS files are big, reserve a single storage for each file
        final MemoryMapStorage storage = MemoryMapStorage.forRawDataFile();

        RawDataFile newMZmineFile = MZmineCore.createNewFile(newName,
            fileNames[i].getAbsolutePath(), storage);
        Task newTask = new WatersImportTask(project, fileNames[i], newMZmineFile,
            WatersImportModule.class, parameters, moduleCallDate);
        tasks.add(newTask);

      }
      catch (Exception e) {
        e.printStackTrace();
        MZmineCore.getDesktop().displayErrorMessage("Could not create a new temporary file " + e.getMessage());
        logger.log(Level.SEVERE, "Could not create a new temporary file ", e.getMessage());
        return ExitCode.ERROR;
      }
      }
    return ExitCode.OK;
  }

}
