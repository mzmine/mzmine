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

package io.github.mzmine.modules.io.import_rawdata_bruker_tsf;

import com.google.common.base.Strings;
import io.github.mzmine.datamodel.MZmineProject;
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
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TSFImportModule implements MZmineProcessingModule {

  private static Logger logger = Logger.getLogger(TSFImportModule.class.getName());

  @NotNull
  @Override
  public String getName() {
    return "TSF import module";
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return TSFImportParameters.class;
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Imports Bruker .d directories with tsf data files.";
  }

  @NotNull
  @Override
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    File fileNames[] = parameters.getParameter(TDFImportParameters.fileNames).getValue();
    final MemoryMapStorage storage = MemoryMapStorage.forRawDataFile();

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

      Task newTask = new TSFImportTask(project, fileNames[i], storage, TSFImportModule.class,
          parameters, moduleCallDate);
      tasks.add(newTask);
    }

    return ExitCode.OK;
  }

  @NotNull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return null;
  }
}
