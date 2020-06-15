/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.masscalibration;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleMassList;
import io.github.mzmine.modules.dataprocessing.masscalibration.standardslist.StandardsList;
import io.github.mzmine.modules.dataprocessing.masscalibration.standardslist.StandardsListExtractor;
import io.github.mzmine.modules.dataprocessing.masscalibration.standardslist.StandardsListExtractorFactory;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public class MassCalibrationTask extends AbstractTask {

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final RawDataFile dataFile;
  // User parameters
  private final String massListName;
  private final String suffix;
  private final boolean autoRemove;
  private final ParameterSet parameters;
  // scan counter
  protected int processedScans = 0, totalScans;
  protected int[] scanNumbers;
  private StandardsListExtractor standardsListExtractor;

  /**
   * @param dataFile
   * @param parameters
   */
  public MassCalibrationTask(RawDataFile dataFile, ParameterSet parameters) {

    this.dataFile = dataFile;
    this.parameters = parameters;

    this.massListName = parameters.getParameter(MassCalibrationParameters.massList).getValue();

    this.suffix = parameters.getParameter(MassCalibrationParameters.suffix).getValue();
    this.autoRemove = parameters.getParameter(MassCalibrationParameters.autoRemove).getValue();

  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  public String getTaskDescription() {
    return "Calibrating mass in " + dataFile;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    if (totalScans == 0)
      return 0;
    else
      // processed scans are added twice, when errors are obtain and when mass lists are shifted
      // so to get finished percentage of the task, divide processed scans by double total scans
      return (double) processedScans / totalScans / 2;
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  /**
   * @see Runnable#run()
   */
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Started mass calibration on " + dataFile);

    String standardsListFilename = null;
    StandardsList standardsList;
    try {
      standardsListFilename = parameters.getParameter(MassCalibrationParameters.standardsList).getValue().getName();
      standardsListExtractor = StandardsListExtractorFactory.createFromFilename(standardsListFilename);
      standardsList = standardsListExtractor.extractStandardsList();
    } catch (Exception e) {
      logger.warning("Exception when extracting standards list from " + standardsListFilename);
      logger.warning(e.toString());
      setStatus(TaskStatus.ERROR);
      return;
    }

    Double tolerance = parameters.getParameter(MassCalibrationParameters.tolerance).getValue();
    Double mzRatioTolerance = parameters.getParameter(MassCalibrationParameters.mzRatioTolerance).getValue();
    Double rtTolerance = parameters.getParameter(MassCalibrationParameters.retentionTimeSecTolerance).getValue();

    MassCalibrator massCalibrator = new MassCalibrator(rtTolerance, mzRatioTolerance, tolerance, standardsList);

    ArrayList<Double> errors = new ArrayList<Double>();

    scanNumbers = dataFile.getScanNumbers();
    totalScans = scanNumbers.length;

    // Check if we have at least one scan with a mass list of given name
    boolean haveMassList = false;
    for (int i = 0; i < totalScans; i++) {
      Scan scan = dataFile.getScan(scanNumbers[i]);
      MassList massList = scan.getMassList(massListName);
      if (massList != null) {
        haveMassList = true;
        break;
      }
    }
    if (!haveMassList) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage(dataFile.getName() + " has no mass list called '" + massListName + "'");
      return;
    }

    // obtain errors from all scans
    for (int i = 0; i < totalScans; i++) {

      if (isCanceled())
        return;

      Scan scan = dataFile.getScan(scanNumbers[i]);

      MassList massList = scan.getMassList(massListName);

      // Skip those scans which do not have a mass list of given name
      if (massList == null) {
        processedScans++;
        continue;
      }

      DataPoint[] mzPeaks = massList.getDataPoints();

      List<Double> massListErrors = massCalibrator.findMassListErrors(mzPeaks, scan.getRetentionTime() * 60);
      errors.addAll(massListErrors);

      processedScans++;
    }

    double biasEstimate = massCalibrator.estimateBiasFromErrors(errors);

    // mass calibrate all mass lists
    for (int i = 0; i < totalScans; i++) {

      if (isCanceled())
        return;

      Scan scan = dataFile.getScan(scanNumbers[i]);

      MassList massList = scan.getMassList(massListName);

      // Skip those scans which do not have a mass list of given name
      if (massList == null) {
        processedScans++;
        continue;
      }

      DataPoint[] mzPeaks = massList.getDataPoints();

      DataPoint[] newMzPeaks = massCalibrator.calibrateMassList(mzPeaks, biasEstimate);

      SimpleMassList newMassList =
              new SimpleMassList(massListName + " " + suffix, scan, newMzPeaks);

      scan.addMassList(newMassList);

      // Remove old mass list
      if (autoRemove)
        scan.removeMassList(massList);

      processedScans++;
    }

    setStatus(TaskStatus.FINISHED);

    logger.info("Finished mass calibration on " + dataFile);

  }

}
