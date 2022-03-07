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
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RawDataFileType;
import io.github.mzmine.util.StreamCopy;
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
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

public class ZipImportTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final File fileToOpen;
  private final @NotNull MZmineProject project;
  private final RawDataFileType fileType;
  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;

  private File tmpDir, tmpFile;
  private StreamCopy copy = null;
  private Task decompressedOpeningTask = null;

  public ZipImportTask(@NotNull MZmineProject project, File fileToOpen, RawDataFileType fileType,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // storage in raw data file
    this.project = project;
    this.fileToOpen = fileToOpen;
    this.fileType = fileType;
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

      // Name of the uncompressed file
      String newName = fileToOpen.getName();
      if (newName.toLowerCase().endsWith(".zip") || newName.toLowerCase().endsWith(".gz")) {
        newName = FilenameUtils.removeExtension(newName);
      }

      // Create decompressing stream
      FileInputStream fis = new FileInputStream(fileToOpen);
      InputStream is;
      long decompressedSize = 0;
      switch (fileType) {
        case ZIP:
          ZipInputStream zis = new ZipInputStream(fis);
          ZipEntry entry = zis.getNextEntry();
          newName = entry.getName();
          decompressedSize = entry.getSize();
          if (decompressedSize < 0) {
            decompressedSize = 0;
          }
          is = zis;
          break;
        case GZIP:
          is = new GZIPInputStream(fis);
          // Ballpark a decompressedFile size so the GUI can show progress
          decompressedSize = (long) (fileToOpen.length() * 1.5);
          if (decompressedSize < 0) {
            decompressedSize = 0;
          }
          break;
        default:
          setErrorMessage("Cannot decompress file type: " + fileType);
          setStatus(TaskStatus.ERROR);
          return;
      }

      BufferedInputStream bis = new BufferedInputStream(is);
      final MSDKmzMLImportTask msdKmzMLImportTask = new MSDKmzMLImportTask(project, fileToOpen, bis,
          MZmineCore.createNewFile(fileToOpen.getName(), fileToOpen.getAbsolutePath(),
              MemoryMapStorage.forRawDataFile()), null, ZipImportModule.class, parameters,
          getModuleCallDate());

      if (isCanceled()) {
        return;
      }

      decompressedOpeningTask = msdKmzMLImportTask;

      // Run the underlying task
      decompressedOpeningTask.run();

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
      return "Decompressing file " + fileToOpen;
    }
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if(decompressedOpeningTask == null) {
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
    if (copy != null) {
      copy.cancel();
    }
    if ((tmpFile != null) && (tmpFile.exists())) {
      tmpFile.delete();
    }
    if ((tmpDir != null) && (tmpDir.exists())) {
      tmpDir.delete();
    }
  }

}
