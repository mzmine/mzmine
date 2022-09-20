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
