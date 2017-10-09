/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
package net.sf.mzmine.modules.rawdatamethods.peakpicking.targetedpeakdetection;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection.centroid.CentroidMassDetectorParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

import com.Ostermiller.util.CSVParser;
import com.google.common.collect.Range;

class TargetedPeakDetectionModuleTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private final RawDataFile dataFile;
  private PeakList processedPeakList;
  private String suffix;
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  private double intTolerance;
  private ParameterSet parameters;
  private int processedScans, totalScans;
  private File peakListFile;
  private String fieldSeparator;
  private boolean ignoreFirstLine;
  private int finishedLines = 0;
  private int ID = 1;
  private double noiseLevel;

  TargetedPeakDetectionModuleTask(MZmineProject project, ParameterSet parameters,
      RawDataFile dataFile) {

    this.project = project;
    this.parameters = parameters;

    suffix = parameters.getParameter(TargetedPeakDetectionParameters.suffix).getValue();
    peakListFile = parameters.getParameter(TargetedPeakDetectionParameters.peakListFile).getValue();
    fieldSeparator =
        parameters.getParameter(TargetedPeakDetectionParameters.fieldSeparator).getValue();
    ignoreFirstLine =
        parameters.getParameter(TargetedPeakDetectionParameters.ignoreFirstLine).getValue();

    intTolerance = parameters.getParameter(TargetedPeakDetectionParameters.intTolerance).getValue();
    mzTolerance = parameters.getParameter(TargetedPeakDetectionParameters.MZTolerance).getValue();
    rtTolerance = parameters.getParameter(TargetedPeakDetectionParameters.RTTolerance).getValue();
    noiseLevel = parameters.getParameter(CentroidMassDetectorParameters.noiseLevel).getValue();

    this.dataFile = dataFile;
  }

  public void run() {

    setStatus(TaskStatus.PROCESSING);

    // Calculate total number of scans in all files
    totalScans = dataFile.getNumOfScans(1);

    // Create new peak list
    processedPeakList = new SimplePeakList(dataFile.getName() + " " + suffix, dataFile);

    List<PeakInformation> peaks = this.readFile();

    if (peaks == null || peaks.isEmpty()) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not read file or the file is empty ");
      return;
    }
    // Fill new peak list with empty rows
    for (int row = 0; row < peaks.size(); row++) {
      PeakListRow newRow = new SimplePeakListRow(ID++);
      processedPeakList.addRow(newRow);
    }

    // Process all raw data files

    // Canceled?
    if (isCanceled()) {
      return;
    }

    List<Gap> gaps = new ArrayList<Gap>();

    // Fill each row of this raw data file column, create new empty
    // gaps if necessary
    for (int row = 0; row < peaks.size(); row++) {
      PeakListRow newRow = processedPeakList.getRow(row);
      // Create a new gap

      Range<Double> mzRange = mzTolerance.getToleranceRange(peaks.get(row).getMZ());
      Range<Double> rtRange = rtTolerance.getToleranceRange(peaks.get(row).getRT());
      newRow.addPeakIdentity(new SimplePeakIdentity(peaks.get(row).getName()), true);

      Gap newGap = new Gap(newRow, dataFile, mzRange, rtRange, intTolerance, noiseLevel);

      gaps.add(newGap);

    }

    // Stop processing this file if there are no gaps
    if (gaps.isEmpty()) {
      processedScans += dataFile.getNumOfScans();
    }

    // Get all scans of this data file
    int scanNumbers[] = dataFile.getScanNumbers(1);

    // Process each scan
    for (int scanNumber : scanNumbers) {

      // Canceled?
      if (isCanceled()) {
        return;
      }

      // Get the scan
      Scan scan = dataFile.getScan(scanNumber);

      // Feed this scan to all gaps
      for (Gap gap : gaps) {
        gap.offerNextScan(scan);
      }

      processedScans++;
    }

    // Finalize gaps
    for (Gap gap : gaps) {
      gap.noMoreOffers();
    }

    // Append processed peak list to the project
    project.addPeakList(processedPeakList);

    // Add quality parameters to peaks
    QualityParameters.calculateQualityParameters(processedPeakList);

    // Add task description to peakList
    processedPeakList.addDescriptionOfAppliedTask(
        new SimplePeakListAppliedMethod("Targeted peak detection ", parameters));

    logger.log(Level.INFO, "Targeted peak detection on {0}", this.dataFile);
    setStatus(TaskStatus.FINISHED);

  }

  public List<PeakInformation> readFile() {
    FileReader dbFileReader = null;
    try {
      List<PeakInformation> list = new ArrayList<PeakInformation>();
      dbFileReader = new FileReader(peakListFile);

      String[][] peakListValues = CSVParser.parse(dbFileReader, fieldSeparator.charAt(0));

      if (ignoreFirstLine) {
        finishedLines++;
      }
      for (; finishedLines < peakListValues.length; finishedLines++) {
        try {
          // Removing the FEFF character is important in case the CSV file contains byte-order-mark
          String mzString = peakListValues[finishedLines][0].replace("\uFEFF", "").trim();
          String rtString = peakListValues[finishedLines][1].replace("\uFEFF", "").trim();
          double mz = Double.parseDouble(mzString);
          double rt = Double.parseDouble(rtString);
          String name = peakListValues[finishedLines][2].trim();
          list.add(new PeakInformation(mz, rt, name));
        } catch (Exception e) {
          e.printStackTrace();
          // ignore incorrect lines
        }
      }
      dbFileReader.close();
      return list;

    } catch (Exception e) {
      e.printStackTrace();
      logger.log(Level.WARNING, "Could not read file " + peakListFile, e);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(e.toString());
      return null;
    }

  }

  public double getFinishedPercentage() {
    if (totalScans == 0) {
      return 0;
    }
    return (double) processedScans / (double) totalScans;

  }

  public String getTaskDescription() {
    return "Targeted peak detection " + this.dataFile;
  }

}
