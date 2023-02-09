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

package io.github.mzmine.modules.io.import_rawdata_all;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.ScanUtils;
import java.time.Instant;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * This import task wraps other data import tasks that do not support application of mass detection
 * during data import. This task calls the data import and applies mass detection afterwards.
 */
public class MsDataImportAndMassDetectWrapperTask extends AbstractTask {

  private final RawDataFile newMZmineFile;
  private final AbstractTask importTask;
  private final Boolean denormalizeMSnScans;
  private MZmineProcessingStep<MassDetector> ms1Detector = null;
  private MZmineProcessingStep<MassDetector> ms2Detector = null;

  private int totalScans = 1;
  private final int parsedScans = 0;

  /**
   * This import task wraps other data import tasks that do not support application of mass
   * detection during data import. This task calls the data import and applies mass detection
   * afterwards.
   *
   * @param storageMassLists data storage for mass lists (usually different to that of the data
   *                         file
   * @param newMZmineFile    the resulting data file
   * @param importTask       the data import task (that does not support the advanced import option
   *                         to directly centroid/threshold)
   * @param advancedParam    advanced parameters to apply mass detection
   */
  public MsDataImportAndMassDetectWrapperTask(MemoryMapStorage storageMassLists,
      RawDataFile newMZmineFile, AbstractTask importTask, @NotNull ParameterSet advancedParam,
      @NotNull Instant moduleCallDate) {
    super(storageMassLists, moduleCallDate);
    this.newMZmineFile = newMZmineFile;
    this.importTask = importTask;

    if (advancedParam.getParameter(AdvancedSpectraImportParameters.msMassDetection).getValue()) {
      this.ms1Detector = advancedParam.getParameter(AdvancedSpectraImportParameters.msMassDetection)
          .getEmbeddedParameter().getValue();
    }
    if (advancedParam.getParameter(AdvancedSpectraImportParameters.ms2MassDetection).getValue()) {
      this.ms2Detector = advancedParam.getParameter(
          AdvancedSpectraImportParameters.ms2MassDetection).getEmbeddedParameter().getValue();
    }
    denormalizeMSnScans = advancedParam.getValue(
        AdvancedSpectraImportParameters.denormalizeMSnScans);
  }

  @Override
  public String getTaskDescription() {
    return importTask.isFinished() || importTask.isCanceled() ? "Applying mass detection on "
        + newMZmineFile.getName() : importTask.getTaskDescription();
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
        totalScans = newMZmineFile.getNumOfScans();

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
    // uses only a single array for each (mz and intensity) to loop over all scans
    ScanDataAccess data = EfficientDataAccess.of(newMZmineFile,
        EfficientDataAccess.ScanDataType.RAW);
    totalScans = data.getNumberOfScans();

    // all scans
    while (data.hasNextScan()) {
      if (isCanceled() || (importTask != null && importTask.isCanceled())) {
        return false;
      }

      Scan scan = data.nextScan();

      int msLevel = Objects.requireNonNullElse(scan.getMSLevel(), 1);
      double[][] mzIntensities = null;
      if (ms1Detector != null && msLevel <= 1) {
        mzIntensities = ms1Detector.getModule().getMassValues(data, ms1Detector.getParameterSet());
      } else if (ms2Detector != null && msLevel >= 2) {
        mzIntensities = ms2Detector.getModule().getMassValues(data, ms2Detector.getParameterSet());
        if (denormalizeMSnScans) {
          ScanUtils.denormalizeIntensitiesMultiplyByInjectTime(mzIntensities[1],
              scan.getInjectionTime());
        }
      }

      if (mzIntensities != null) {
        // uses a different storage for mass lists then the one defined for the MS data import
        SimpleMassList newMassList = new SimpleMassList(storage, mzIntensities[0],
            mzIntensities[1]);
        scan.addMassList(newMassList);
      }
    }
    return true;
  }


}
