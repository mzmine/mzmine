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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.ScanUtils;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class MassDetectionTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(MassDetectionTask.class.getName());
  private final RawDataFile dataFile;
  private final ScanSelection scanSelection;
  private final SelectedScanTypes scanTypes;
  private final Boolean denormalizeMSnScans;
  private final ParameterSet parameters;
  private final MassDetector detector;
  private int processedScans = 0, totalScans = 0;

  public MassDetectionTask(RawDataFile dataFile, ParameterSet parameters,
      MemoryMapStorage storageMemoryMap, @NotNull Instant moduleCallDate) {
    super(storageMemoryMap, moduleCallDate);

    this.dataFile = dataFile;

    var md = parameters.getParameter(MassDetectionParameters.massDetector).getValueWithParameters();
    detector = MassDetectors.createMassDetector(md);

    this.scanSelection = parameters.getValue(MassDetectionParameters.scanSelection);
    this.scanTypes = parameters.getValue(MassDetectionParameters.scanTypes);
    denormalizeMSnScans = parameters.getValue(MassDetectionParameters.denormalizeMSnScans);

    this.parameters = parameters;

  }

  @Override
  public String getTaskDescription() {
    return "Detecting masses in " + dataFile;
  }

  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0 : (double) processedScans / totalScans;
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  @Override
  public void run() {
    try {

      setStatus(TaskStatus.PROCESSING);

      logger.info("Started mass detector on " + dataFile);

      // uses only a single array for each (mz and intensity) to loop over all scans
      ScanDataAccess data = EfficientDataAccess.of(dataFile, EfficientDataAccess.ScanDataType.RAW,
          scanSelection);
      totalScans = data.getNumberOfScans();

      // all scans
      while (data.hasNextScan()) {
        if (isCanceled()) {
          return;
        }

        Scan scan = data.nextScan();
        assert scan != null;

        double[][] mzPeaks;
        if (scanTypes.applyTo(scan)) {
          // run mass detection on data object
          // [mzs, intensities]
          mzPeaks = detector.getMassValues(data);

          // denormalize scan intensities if injection time of trapped instrument was used.
          // this is only done for MS2 because absolute intensities do not matter there
          // MS1 needs to be normalized by injection time, which is already done during data acquisition
          if (denormalizeMSnScans && scan.getMSLevel() > 1) {
            ScanUtils.denormalizeIntensitiesMultiplyByInjectTime(mzPeaks[1],
                scan.getInjectionTime());
          }

          // add mass list to scans and frames
          scan.addMassList(new SimpleMassList(getMemoryMapStorage(), mzPeaks[0], mzPeaks[1]));
        }

        if (scan instanceof SimpleFrame frame && (scanTypes == SelectedScanTypes.MOBLITY_SCANS
                                                  || scanTypes == SelectedScanTypes.SCANS)) {
          // for ion mobility, detect subscans, too
          frame.getMobilityScanStorage()
              .generateAndAddMobilityScanMassLists(getMemoryMapStorage(), detector,
                  denormalizeMSnScans);
        }

        processedScans++;
      }

      dataFile.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(MassDetectionModule.class, parameters,
              getModuleCallDate()));
    } catch (Exception e) {
      logger.log(Level.WARNING, "Error during mass detection, " + e.getMessage(), e);
      setErrorMessage(e.getMessage());
      setStatus(TaskStatus.ERROR);
      return;
    }

    setStatus(TaskStatus.FINISHED);

    logger.info("Finished mass detector on " + dataFile);
  }
}
