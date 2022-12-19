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
package io.github.mzmine.modules.dataprocessing.featdet_msn;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class MsnPeakPickingTask extends AbstractTask {

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final MZmineProject project;
  private final RawDataFile dataFile;
  private final ScanSelection scanSelection;
  private final int msLevel;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final ParameterSet parameterSet;
  private final Scan[] scans;
  private final ModularFeatureList newFeatureList;
  private int processedScans, totalScans;

  public MsnPeakPickingTask(MZmineProject project, RawDataFile dataFile, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.dataFile = dataFile;

    scanSelection = parameters.getParameter(MsnPeakPickerParameters.scanSelection).getValue();
    msLevel = parameters.getParameter(MsnPeakPickerParameters.msLevel).getValue();
    mzTolerance = parameters.getParameter(MsnPeakPickerParameters.mzDifference).getValue();
    rtTolerance = parameters.getParameter(MsnPeakPickerParameters.rtTolerance).getValue();
    scans = scanSelection.getMatchingScans(dataFile);
    newFeatureList = new ModularFeatureList(dataFile.getName() + " MSn features",
        getMemoryMapStorage(), dataFile);
    this.parameterSet = parameters;
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalScans == 0) {
      return 0f;
    }
    return (double) processedScans / totalScans;
  }

  @Override
  public String getTaskDescription() {
    return "Building MSn feature list based on MSn from " + dataFile;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    int[] totalMSLevel = dataFile.getMSLevels();

    // No MSn scan in datafile.
    if (!ArrayUtils.contains(totalMSLevel, msLevel)) {
      setStatus(TaskStatus.ERROR);
      final String msg = "No MS" + msLevel + " scans in " + dataFile.getName();
      setErrorMessage(msg);
      return;
    }

    final Scan scans[] = scanSelection.getMatchingScans(dataFile);
    totalScans = scans.length;

    // No scans in selection range.
    if (totalScans == 0) {
      setStatus(TaskStatus.ERROR);
      final String msg = "No scans detected in selection range for " + dataFile.getName();
      setErrorMessage(msg);
      return;
    }

    /**
     * Process each MS2 scan to find MSn scans through fragmentationScan tracing. If a MSn scan
     * found, build simple modular feature for MS2 precursor in range.
     */
    for (Scan scan : scans) {

      // Canceled?
      if (isCanceled()) {
        return;
      }

      // MSn scans will be found through MS2 fragmentScan linking.
      if (scan.getMSLevel() != 2) {
        processedScans++;
        continue;
      }

      // Does scan possess MSn scans?
      boolean validScan = false;

      // If mslevel is 2, true by default.
      if (scan.getMSLevel() == msLevel) {
        validScan = true;
      } else {

        // Search for MSn Scans.
        int[] scanList = getMSnScanNumbers(scan);

        if (scanList != null) {
          validScan = true;
        }
      }

      // If valid, build simple feature for precursor.
      if (validScan) {

        // Get ranges.
        float scanRT = scan.getRetentionTime();
        double precursorMZ =
            scan.getMsMsInfo() != null && scan.getMsMsInfo() instanceof DDAMsMsInfo dda
                ? dda.getIsolationMz() : 0d;

        Range<Float> rtRange = rtTolerance.getToleranceRange(scanRT);
        Range<Double> mzRange = mzTolerance.getToleranceRange(precursorMZ);

        // Build simple feature for precursor in ranges.
        ModularFeature newFeature = FeatureUtils.buildSimpleModularFeature(newFeatureList, dataFile,
            rtRange, mzRange);

        // Add feature to feature list.
        if (newFeature != null) {

          ModularFeatureListRow newFeatureListRow = new ModularFeatureListRow(newFeatureList,
              scan.getScanNumber(), newFeature);

          newFeatureList.addRow(newFeatureListRow);
        }
      }

      processedScans++;
    }

    // No MSn features detected in range.
    if (newFeatureList.isEmpty()) {
      setStatus(TaskStatus.ERROR);
      final String msg =
          "No MSn precursor features detected in selected range for " + dataFile.getName();
      setErrorMessage(msg);
      return;
    }

    dataFile.getAppliedMethods().forEach(m -> newFeatureList.getAppliedMethods().add(m));
    newFeatureList.setSelectedScans(dataFile, List.of(scans));
    newFeatureList.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(MsnFeatureDetectionModule.class, parameterSet,
            getModuleCallDate()));

    // Add new feature list to the project
    project.addFeatureList(newFeatureList);

    logger.info(
        "Finished MSn feature builder on " + dataFile + ", " + processedScans + " scans processed");

    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Get scan numbers for input MS level.
   *
   * @param scan
   * @return
   */
  private int[] getMSnScanNumbers(Scan scan) {

    int[] allLevels = dataFile.getMSLevels();

    // MS level not in data file.
    if (!ArrayUtils.contains(allLevels, msLevel)) {
      return null;
    }

//    if (scan.getPrecursorMZ() == 0) {
//      return null;
//    }

    // int[] fragmentScanNumbers = scan.getFragmentScanNumbers();
    //
    // // Recursively search fragment scans for all scan numbers at MS level.
    // if (fragmentScanNumbers != null) {
    //
    // // Return MSn fragment scans numbers if they exist.
    // if (scan.getMSLevel() + 1 == msLevel) {
    // return fragmentScanNumbers;
    // } else {
    //
    // // Array for all MSn scan numbers.
    // int[] msnScanNumbers = {};
    //
    // // Recursively search fragment scan chain.
    // for (int fScanNum : fragmentScanNumbers) {
    //
    // Scan fragmentScan = dataFile.getScan(fScanNum);
    //
    // int[] foundScanNumbers = getMSnScanNumbers(fragmentScan);
    //
    // if (foundScanNumbers != null) {
    //
    // msnScanNumbers = ArrayUtils.addAll(msnScanNumbers, foundScanNumbers);
    // } else {
    // return null;
    // }
    // }
    //
    // return msnScanNumbers;
    // }
    // }

    // No fragment scans found.
    return null;
  }

}
