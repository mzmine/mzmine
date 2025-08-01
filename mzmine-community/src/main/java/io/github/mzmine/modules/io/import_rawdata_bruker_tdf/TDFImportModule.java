/*
 * Copyright (c) 2004-2024 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf;

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
public class TDFImportModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Bruker TDF file import";
  private static final String MODULE_DESCRIPTION = "This module imports raw data into the project.";
  private static final Logger logger = Logger.getLogger(TDFImportModule.class.getName());

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
    return TDFImportParameters.class;
  }

  @Override
  @NotNull
  public ExitCode runModule(final @NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {

    File fileNames[] = parameters.getParameter(TDFImportParameters.fileNames).getValue();
    final MemoryMapStorage storage = MemoryMapStorage.forRawDataFile();

    if (Arrays.asList(fileNames).contains(null)) {
      logger.warning("List of filenames contains null");
      return ExitCode.ERROR;
    }

    for (int i = 0; i < fileNames.length; i++) {

      if ((!fileNames[i].exists()) || (!fileNames[i].canRead())) {
        MZmineCore.getDesktop().displayErrorMessage("Cannot read file " + fileNames[i]);
        logger.warning("Cannot read file " + fileNames[i]);
        return ExitCode.ERROR;
      }

      Task newTask = new TDFImportTask(project, fileNames[i], storage, TDFImportModule.class,
          parameters, moduleCallDate);
      tasks.add(newTask);

    }

    return ExitCode.OK;
  }

}
