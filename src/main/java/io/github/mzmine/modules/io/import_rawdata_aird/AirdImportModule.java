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

package io.github.mzmine.modules.io.import_rawdata_aird;

import com.google.common.base.Strings;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RawDataFileUtils;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import net.csibio.aird.util.AirdScanUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Raw data import module
 */
public class AirdImportModule implements MZmineProcessingModule {

  private static final Logger logger = Logger.getLogger(AirdImportModule.class.getName());
  private static final String MODULE_NAME = "Aird file import";
  private static final String MODULE_DESCRIPTION = "This module imports aird raw data into the project.";

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
    return AirdImportParameters.class;
  }

  @Override
  @NotNull
  public ExitCode runModule(final @NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {

    File files[] = parameters.getParameter(AirdImportParameters.fileNames).getValue();

    if (Arrays.asList(files).contains(null)) {
      logger.warning("List of filenames contains null");
      return ExitCode.ERROR;
    }

    // Find common prefix in raw file names if in GUI mode
    String commonPrefix = RawDataFileUtils.askToRemoveCommonPrefix(files);

    // one storage for all files imported in the same task as they are typically analyzed together
    final MemoryMapStorage storage = MemoryMapStorage.forRawDataFile();

    for (int i = 0; i < files.length; i++) {
      File airdFile = files[i];
      String indexFilePath = AirdScanUtil.getIndexPathByAirdPath(airdFile.getPath());
      if (indexFilePath == null) {
        return ExitCode.ERROR;
      }
      File indexFile = new File(indexFilePath);
      if ((!airdFile.exists()) || (!airdFile.canRead())) {
        MZmineCore.getDesktop().displayErrorMessage("Cannot read file " + airdFile);
        logger.warning("Cannot read aird file " + airdFile);
        return ExitCode.ERROR;
      }

      if ((!indexFile.exists()) || (!indexFile.canRead())) {
        MZmineCore.getDesktop().displayErrorMessage("Cannot read file " + indexFile);
        logger.warning("Cannot read index file " + indexFile);
        return ExitCode.ERROR;
      }

      // Set the new name by removing the common prefix
      String newName;
      if (!Strings.isNullOrEmpty(commonPrefix)) {
        final String regex = "^" + Pattern.quote(commonPrefix);
        newName = airdFile.getName().replaceFirst(regex, "");
      } else {
        newName = airdFile.getName();
      }

      try {
        RawDataFile newMZmineFile = MZmineCore.createNewFile(newName, indexFile.getAbsolutePath(),
            storage);
        Task newTask = new AirdImportTask(project, indexFile, newMZmineFile, AirdImportModule.class,
            parameters, moduleCallDate);
        tasks.add(newTask);
      } catch (IOException e) {
        e.printStackTrace();
        MZmineCore.getDesktop().displayErrorMessage("Could not create a new temporary file " + e);
        logger.log(Level.SEVERE, "Could not create a new temporary file ", e);
        return ExitCode.ERROR;
      }


    }

    return ExitCode.OK;
  }

}
