/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.projectmetadata;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.time.Instant;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectMetadataImportTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(ProjectMetadataImportTask.class.getName());
  private File[] files;
  private final int totalFilesNumber;
  private int importedFilesNumber = 0;

  protected ProjectMetadataImportTask(File[] files,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.files = files;
    this.totalFilesNumber = files.length;
  }

  @Override
  public String getTaskDescription() {
    return "Importing metadata parameters from the selected file(s)";
  }

  @Override
  public double getFinishedPercentage() {
    return totalFilesNumber == 0 ? 0 : (double) importedFilesNumber / totalFilesNumber;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    MetadataTable metadataTable = MZmineCore.getProjectManager().getCurrentProject().getProjectMetadata();
    // try to import parameters from each selected .tsv file
    for (File fileName : files) {
      if ((!fileName.exists()) || (!fileName.canRead())) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Cannot read file " + fileName);
        logger.warning("Cannot read file " + fileName);
        return;
      }

      if (metadataTable.importMetadata(fileName, false)) {
        logger.info("Successfully imported parameters from " + fileName);
      } else {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Couldn't import metadata from " + fileName);
        logger.warning("Error while processing " + fileName);
        return;
      }

      importedFilesNumber++;
    }

    setStatus(TaskStatus.FINISHED);
  }
}
