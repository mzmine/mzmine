/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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
package io.github.mzmine.modules.dataprocessing.filter_scan_signals;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.MissingMassListException;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class ScanSignalRemovalTask extends AbstractTask {

  private final RawDataFile dataFile;
  private final ScanSelection scanSelection;
  private final ParameterSet parameterSet;
  private final RangeSet<Double> removeMzRanges;

  private final DoubleList mzs = new DoubleArrayList();
  private final DoubleList intensities = new DoubleArrayList();
  private int processedScans, totalScans;

  public ScanSignalRemovalTask(RawDataFile dataFile, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.dataFile = dataFile;
    this.parameterSet = parameters;

    scanSelection = parameters.getValue(ScanSignalRemovalParameters.scanSelection);
    List<Range<Double>> ranges = parameters.getValue(ScanSignalRemovalParameters.removeMzRanges);

    removeMzRanges = TreeRangeSet.create(ranges);
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0 : (double) processedScans / totalScans;
  }

  @Override
  public String getTaskDescription() {
    return "Removing m/z ranges from " + dataFile + " " + removeMzRanges.toString();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    try {
      if (dataFile instanceof IMSRawDataFile imsFile) {
        // frames
        processFileScans();
        // mobility scans
        processImsFileMobilityScans(imsFile);
      } else {
        // scans
        processFileScans();
      }
      if (isCanceled()) {
        return;
      }

      dataFile.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(ScanSignalRemovalModule.class, parameterSet,
              getModuleCallDate()));
    } catch (MissingMassListException ex) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage(ex.getMessage());
      return;
    }

    setStatus(TaskStatus.FINISHED);
  }

  private void processFileScans() {
    ScanDataAccess scans = EfficientDataAccess.of(dataFile, ScanDataType.MASS_LIST, scanSelection);
    totalScans = scans.getNumberOfScans();

    while (scans.hasNextScan()) {
      if (isCanceled()) {
        return;
      }
      scans.nextScan();

      processScan(scans);

      var massList = new SimpleMassList(storage, mzs.toDoubleArray(), intensities.toDoubleArray());
      scans.addMassList(massList);

      mzs.clear();
      intensities.clear();

      processedScans++;
    }
  }

  /**
   * TODO seems to not work for IMS data
   *
   * @param imsFile
   */
  private void processImsFileMobilityScans(IMSRawDataFile imsFile) {
    MobilityScanDataAccess frameIterator = EfficientDataAccess.of(imsFile,
        MobilityScanDataType.MASS_LIST, scanSelection);

    totalScans = frameIterator.getNumberOfScans();

    List<double[][]> frameData = new ArrayList<>();

    while (frameIterator.hasNextFrame()) {
      frameIterator.nextFrame();
      while (frameIterator.nextMobilityScan() != null) {
        if (isCanceled()) {
          return;
        }

        processScan(frameIterator);

        frameData.add(new double[][]{mzs.toDoubleArray(), intensities.toDoubleArray()});
        mzs.clear();
        intensities.clear();
      }

      frameIterator.getFrame().getMobilityScanStorage().setMassLists(storage, frameData);
      frameData.clear();
      processedScans++;
    }
  }

  private void processScan(Scan scan) {
    for (int i = 0; i < scan.getNumberOfDataPoints(); i++) {
      double mz = scan.getMzValue(i);
      if (removeMzRanges.contains(mz)) {
        continue;
      }

      double intensity = scan.getIntensityValue(i);

      mzs.add(mz);
      intensities.add(intensity);
    }
  }

}
