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

package io.github.mzmine.modules.dataprocessing.filter_cropfilter;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.ScanUtils;
import java.time.Instant;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CropFilterTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private MZmineProject project;
  private RawDataFile dataFile;
  private int processedScans, totalScans;
  private Scan[] scans;

  // User parameters
  private ScanSelection scanSelection;
  private Range<Double> mzRange;
  private String suffix;

  private boolean emptyScans;
  private boolean removeOriginal;
  private ParameterSet parameters;
  private final MemoryMapStorage storage;

  CropFilterTask(MZmineProject project, RawDataFile dataFile, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.dataFile = dataFile;

    this.scanSelection = parameters.getParameter(CropFilterParameters.scanSelection).getValue();
    this.mzRange = parameters.getParameter(CropFilterParameters.mzRange).getValue();
    this.suffix = parameters.getParameter(CropFilterParameters.suffix).getValue();
    this.emptyScans = parameters.getParameter(CropFilterParameters.emptyScans).getValue();
    this.removeOriginal = parameters.getParameter(CropFilterParameters.autoRemove).getValue();
    this.parameters = parameters;
    this.storage = storage;
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    logger.info("Started crop filter on " + dataFile);

    scans = scanSelection.getMatchingScans(dataFile);
    totalScans = scans.length;

    // Check if we have any scans
    if (totalScans == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No scans match the selected criteria");
      return;
    }

    try {

      RawDataFile newFile = MZmineCore.createNewFile(dataFile.getName() + " " + suffix, null, storage);

      for (Scan scan : scans) {

        SimpleScan scanCopy = null;

        if (scan.isEmptyScan()) {
          logger.finest("Scan " + scan.getScanNumber() + " has empty m/z range");
          if (emptyScans) {
            continue;
          } else {
            scanCopy = new SimpleScan(newFile, scan, scan.getMzValues(new double[0]), scan.getIntensityValues(new double[0]));
            newFile.addScan(scanCopy);
            processedScans++;
            continue;
          }
        }

        // Check if we have something to crop
        if (!mzRange.encloses(scan.getDataPointMZRange())) {
          DataPoint croppedDataPoints[] =
              ScanUtils.selectDataPointsByMass(ScanUtils.extractDataPoints(scan), mzRange);

          double[][] dp = DataPointUtils.getDataPointsAsDoubleArray(croppedDataPoints);
          scanCopy = new SimpleScan(newFile, scan, dp[0], dp[1]);
        } else {
          scanCopy = new SimpleScan(newFile, scan, scan.getMzValues(new double[0]),
              scan.getIntensityValues(new double[0]));
        }

        newFile.addScan(scanCopy);

        processedScans++;
      }

      for (FeatureListAppliedMethod appliedMethod : dataFile.getAppliedMethods()) {
        newFile.getAppliedMethods().add(appliedMethod);
      }
      newFile.getAppliedMethods().add(new SimpleFeatureListAppliedMethod(
          CropFilterModule.class, parameters, getModuleCallDate()));
      project.addFile(newFile);

      // Remove the original file if requested
      if (removeOriginal) {
        project.removeFile(dataFile);
      }

      setStatus(TaskStatus.FINISHED);

    } catch (Exception e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage(e.toString());
      e.printStackTrace();
    }
  }

  @Override
  public double getFinishedPercentage() {
    if (totalScans == 0)
      return 0;
    return (double) processedScans / totalScans;
  }

  @Override
  public String getTaskDescription() {
    return "Cropping file " + dataFile.getName();
  }

}
