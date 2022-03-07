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
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.RawDataFileType;
import io.github.mzmine.util.RawDataFileTypeDetector;
import io.github.mzmine.util.RawDataFileUtils;
import io.github.mzmine.util.StreamCopy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

public class ZipImportTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  public static final String UNZIP_DIR = "mzmine_unzip";

  private final File fileToOpen;
  private final @NotNull MZmineProject project;
  private final RawDataFileType fileType;
  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;

  private File tmpDir, tmpFile;
  private StreamCopy copy = null;
  private Task decompressedOpeningTask = null;

  public ZipImportTask(@NotNull MZmineProject project, File fileToOpen, RawDataFileType fileType,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters, @NotNull Instant moduleCallDate) {
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

      tmpDir = Files.createTempDirectory(UNZIP_DIR).toFile();
      tmpFile = new File(tmpDir, newName);
      logger.finest("Decompressing to file " + tmpFile);
      tmpDir.deleteOnExit();
      tmpFile.deleteOnExit();
      FileOutputStream ous = new FileOutputStream(tmpFile);

      // Decompress the contents
      copy = new StreamCopy();
      copy.copy(is, ous, decompressedSize);

      // Close the streams
      is.close();
      ous.close();

      if (isCanceled()) {
        return;
      }

      // Find the type of the decompressed file
      RawDataFileType fileType = RawDataFileTypeDetector.detectDataFileType(tmpFile);
      logger.finest("File " + tmpFile + " type detected as " + fileType);

      if (fileType == null) {
        setErrorMessage("Could not determine the file type of file " + newName);
        setStatus(TaskStatus.ERROR);
        return;
      }

      List<Task> newTasks = new ArrayList<>();
      RawDataFileUtils.createRawDataImportTasks(project, newTasks, module, parameters,
          getModuleCallDate(), tmpFile);
      // Run the import module on the decompressed file
      if (newTasks.size() != 1) {
        setErrorMessage("File type " + fileType + " of file " + newName + " is not supported.");
        setStatus(TaskStatus.ERROR);
        return;
      }
      decompressedOpeningTask = newTasks.get(0);

      // Run the underlying task
      decompressedOpeningTask.run();

      // Delete the temporary folder
      tmpFile.delete();
      tmpDir.delete();

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
    if (decompressedOpeningTask != null) {
      return (decompressedOpeningTask.getFinishedPercentage() / 2.0) + 0.5; // Reports 50% to 100%
    }
    if (copy != null) {
      // Reports up to 50%. In case of .gz files, the uncompressed size
      // was only estimated, so we make sure the progress bar doesn't go
      // over 100%
      double copyProgress = copy.getProgress() / 2.0;
      if (copyProgress > 0.5) {
        copyProgress = 0.5;
      }
      return copyProgress;
    }
    return 0.0;
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
