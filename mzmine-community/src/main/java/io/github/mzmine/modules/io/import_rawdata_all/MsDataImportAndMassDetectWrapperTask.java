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

package io.github.mzmine.modules.io.import_rawdata_all;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataImportTask;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.SimpleSpectralArrays;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This import task wraps other data import tasks that do not support application of mass detection
 * during data import. This task calls the data import and applies mass detection afterwards.
 */
public class MsDataImportAndMassDetectWrapperTask extends AbstractTask implements
    RawDataImportTask {

  private final RawDataImportTask importTask;
  private final ScanImportProcessorConfig scanProcessorConfig;

  private int totalScans = 1;
  private int parsedScans = 0;

  /**
   * This import task wraps other data import tasks that do not support application of mass
   * detection during data import. This task calls the data import and applies mass detection
   * afterwards.
   *
   * @param storageMassLists    data storage for mass lists (usually different to that of the data
   *                            file
   * @param importTask          the data import task (that does not support the advanced import
   *                            option to directly centroid/threshold)
   * @param scanProcessorConfig control processing
   */
  public MsDataImportAndMassDetectWrapperTask(MemoryMapStorage storageMassLists,
      RawDataImportTask importTask,
      @NotNull ScanImportProcessorConfig scanProcessorConfig, @NotNull Instant moduleCallDate) {
    super(storageMassLists, moduleCallDate);
    this.importTask = importTask;
    this.scanProcessorConfig = scanProcessorConfig;
  }

  @Override
  public String getTaskDescription() {
    return importTask.isFinished() || importTask.isCanceled() ? "Applying mass detection."
        : importTask.getTaskDescription();
  }

  @Override
  public double getFinishedPercentage() {
    return totalScans > 0 ? (importTask.getFinishedPercentage() + parsedScans / (double) totalScans)
                            / 2d : importTask.getFinishedPercentage() / 2d;
  }

  @Override
  public void cancel() {
    super.cancel();
    importTask.cancel();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    try {
      // import data
      importTask.run();

      // should be in the new data file
      if (importTask.isFinished()) {
        totalScans = importTask.getImportedRawDataFile().getNumOfScans();

        if (!applyMassDetection()) {
          // cancelled
          return;
        }
      }

    } catch (Exception e) {
      setErrorMessage(e.getMessage());
      setStatus(TaskStatus.ERROR);
      e.printStackTrace();
      return;
    }

    this.setStatus(TaskStatus.FINISHED);
  }

  /**
   * apply mass detection to all scans and sets the mass lists
   *
   * @return true if succeed and false if cancelled
   */
  public boolean applyMassDetection() {
    if (!scanProcessorConfig.hasProcessors()) {
      return true;
    }

    final RawDataFile importedFile = importTask.getImportedRawDataFile();
    totalScans = importedFile.getNumOfScans();

    for (Scan scan : importedFile.getScans()) {
      if (isCanceled() || (importTask != null && importTask.isCanceled())) {
        return false;
      }

      SimpleSpectralArrays processedData = scanProcessorConfig.processor()
          .processScan(scan, new SimpleSpectralArrays(scan));
      // uses a different storage for mass lists then the one defined for the MS data import
      SimpleMassList newMassList = new SimpleMassList(storage, processedData.mzs(),
          processedData.intensities());
      scan.addMassList(newMassList);
      parsedScans++;
    }
    return true;
  }


  @Override
  public @Nullable RawDataFile getImportedRawDataFile() {
    return importTask.getImportedRawDataFile();
  }
}
