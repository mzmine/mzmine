/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_rawdata_zip;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RawDataFileTypeDetector;
import io.github.mzmine.util.exceptions.ExceptionUtils;
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

public class ZipImportTask extends AbstractTask implements RawDataImportTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final File fileToOpen;
  private final @NotNull MZmineProject project;
  private final ScanImportProcessorConfig scanProcessorConfig;
  private final ParameterSet parameters;
  private final Class<? extends MZmineModule> module;

  private MSDKmzMLImportTask msdkTask;

  public ZipImportTask(@NotNull MZmineProject project, File fileToOpen,
      @NotNull final ScanImportProcessorConfig scanProcessorConfig,
      @NotNull final Class<? extends MZmineModule> module, @NotNull final ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable final MemoryMapStorage storage) {
    super(storage, moduleCallDate); // storage in raw data file
    this.project = project;
    this.fileToOpen = fileToOpen;
    this.scanProcessorConfig = scanProcessorConfig;
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

      msdkTask = new MSDKmzMLImportTask(project, fileToOpen, bis, scanProcessorConfig, module,
          parameters, moduleCallDate, getMemoryMapStorage());

      this.addTaskStatusListener((task, newStatus, oldStatus) -> {
        if (isCanceled()) {
          msdkTask.cancel();
        }
      });
      RawDataFile dataFile = msdkTask.importStreamOrFile();

      if (dataFile == null || isCanceled()) {
        return;
      }
      var totalScans = msdkTask.getTotalScansInMzML();
      var parsedScans = msdkTask.getParsedMzMLScans();
      var convertedScans = msdkTask.getConvertedScansAfterFilter();

      bis.close();
      is.close();

      if (isCanceled()) {
        return;
      }

      msdkTask.addAppliedMethodAndAddToProject(dataFile);

    } catch (Throwable e) {
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
    if (msdkTask != null) {
      return msdkTask.getTaskDescription();
    } else {
      return "Importing file " + fileToOpen;
    }
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (msdkTask == null) {
      return 0d;
    }
    return msdkTask.getFinishedPercentage();
  }

  @Override
  public void cancel() {
    super.cancel();
    if (msdkTask != null) {
      msdkTask.cancel();
    }
  }

  @Override
  public RawDataFile getImportedRawDataFile() {
    return getStatus() == TaskStatus.FINISHED ? msdkTask.getImportedRawDataFile() : null;
  }
}
