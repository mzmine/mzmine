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
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RawDataFileTypeDetector;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ZipImportTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final File fileToOpen;
  private final @NotNull MZmineProject project;
  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;

  private Task decompressedOpeningTask = null;

  public ZipImportTask(@NotNull MZmineProject project, File fileToOpen,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable final MemoryMapStorage storage) {
    super(storage, moduleCallDate); // storage in raw data file
    this.project = project;
    this.fileToOpen = fileToOpen;
    this.parameters = parameters;
    this.module = module;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    // Update task status
    setStatus(TaskStatus.PROCESSING);
    logger.info("Started opening compressed file " + fileToOpen);

    try {

      // Create decompressing stream
      FileInputStream fis = new FileInputStream(fileToOpen);
      InputStream is;

      var fileType = RawDataFileTypeDetector.detectDataFileType(fileToOpen);
      switch (fileType) {
        case MZML_ZIP -> {
          ZipInputStream zis = new ZipInputStream(fis);
          final ZipEntry nextEntry = zis.getNextEntry();
          is = zis;
        }
        case MZML_GZIP -> {
          is = new GZIPInputStream(fis);
        }
        default -> {
          setErrorMessage("Cannot decompress file type: " + fileType);
          setStatus(TaskStatus.ERROR);
          return;
        }
      }

      BufferedInputStream bis = new BufferedInputStream(is);
      final MSDKmzMLImportTask msdKmzMLImportTask = new MSDKmzMLImportTask(project, fileToOpen, bis,
          null, ZipImportModule.class, parameters, getModuleCallDate(), getMemoryMapStorage());

      if (isCanceled()) {
        return;
      }

      decompressedOpeningTask = msdKmzMLImportTask;

      // Run the underlying task
      decompressedOpeningTask.run();
      bis.close();
      is.close();

      if (isCanceled()) {
        return;
      }
    } catch (Throwable e) {
      e.printStackTrace();
      logger.log(Level.SEVERE, "Could not open file " + fileToOpen, e);
      setErrorMessage(ExceptionUtils.exceptionToString(e));
      setStatus(TaskStatus.ERROR);
      return;
    }

    logger.info("Finished opening compressed file " + fileToOpen);

    // Update task status
    setStatus(TaskStatus.FINISHED);

  }

  @Override
  public String getTaskDescription() {
    if (decompressedOpeningTask != null) {
      return decompressedOpeningTask.getTaskDescription();
    } else {
      return "Importing file " + fileToOpen;
    }
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (decompressedOpeningTask == null) {
      return 0d;
    }
    return decompressedOpeningTask.getFinishedPercentage();
  }

  @Override
  public void cancel() {
    super.cancel();
    if (decompressedOpeningTask != null) {
      decompressedOpeningTask.cancel();
    }
  }

}
