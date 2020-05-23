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

import java.util.logging.Logger;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleMassList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;

/**
 *
 */
public class MassCalibrationTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private RawDataFile dataFile;

  // User parameters
  private String massListName, suffix;
  private boolean autoRemove;
  private ParameterSet parameters;

  private StandardsListExtractor standardsListExtractor;

  /**
   * @param dataFile
   * @param parameters
   */
  public MassCalibrationTask(RawDataFile dataFile, ParameterSet parameters, 
    StandardsListExtractor standardsListExtractor) {

    this.dataFile = dataFile;
    this.parameters = parameters;

    this.massListName = parameters.getParameter(MassCalibrationParameters.massList).getValue();

    this.suffix = parameters.getParameter(MassCalibrationParameters.suffix).getValue();
    this.autoRemove = parameters.getParameter(MassCalibrationParameters.autoRemove).getValue();

    this.standardsListExtractor = standardsListExtractor;

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
    return 0;
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  /**
   * @see Runnable#run()
   */
  public void run() {

    // System.out.println("RRRRRRRRRRR");
    // System.out.println(this.parameters.getParameter(MassCalibrationParameters.standardsList).getValue());

    setStatus(TaskStatus.PROCESSING);
    logger.info("Started mass calibration on " + dataFile);

    standardsListExtractor.extractStandardsList();

    /*setStatus(TaskStatus.PROCESSING);

    logger.info("Started mass filter on " + dataFile);

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

    // Process all scans
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

      DataPoint mzPeaks[] = massList.getDataPoints();

      DataPoint newMzPeaks[] = ShoulderPeaksFilter.filterMassValues(mzPeaks, parameters);

      SimpleMassList newMassList =
          new SimpleMassList(massListName + " " + suffix, scan, newMzPeaks);

      scan.addMassList(newMassList);

      // Remove old mass list
      if (autoRemove)
        scan.removeMassList(massList);

      processedScans++;
    }

    setStatus(TaskStatus.FINISHED);

    logger.info("Finished shoulder peaks filter on " + dataFile);*/

  }

}
