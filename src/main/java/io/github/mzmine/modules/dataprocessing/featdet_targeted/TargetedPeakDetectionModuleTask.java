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
package io.github.mzmine.modules.dataprocessing.featdet_targeted;

import com.Ostermiller.util.CSVParser;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetectorParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.io.FileReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class TargetedPeakDetectionModuleTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private final RawDataFile dataFile;
  private FeatureList processedPeakList;
  private String suffix;
  private MZTolerance mzTolerance;
  private int msLevel;
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
      RawDataFile dataFile, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.parameters = parameters;

    suffix = parameters.getParameter(TargetedPeakDetectionParameters.suffix).getValue();
    msLevel = parameters.getParameter(TargetedPeakDetectionParameters.msLevel).getValue();
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

    // Create new feature list
    processedPeakList = new ModularFeatureList(dataFile.getName() + " " + suffix, getMemoryMapStorage(), dataFile);

    List<PeakInformation> peaks = this.readFile();

    if (peaks == null || peaks.isEmpty()) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not read file or the file is empty ");
      return;
    }
    // Fill new feature list with empty rows
    for (int row = 0; row < peaks.size(); row++) {
      FeatureListRow newRow = new ModularFeatureListRow((ModularFeatureList) processedPeakList, ID++);
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
      FeatureListRow newRow = processedPeakList.getRow(row);
      // Create a new gap

      Range<Double> mzRange = mzTolerance.getToleranceRange(peaks.get(row).getMZ());
      Range<Float> rtRange = rtTolerance.getToleranceRange((float) peaks.get(row).getRT());
      newRow.addFeatureIdentity(new SimpleFeatureIdentity(peaks.get(row).getName()), true);

      Gap newGap = new Gap(newRow, dataFile, mzRange, rtRange, intTolerance, noiseLevel);
      gaps.add(newGap);
    }

    // Stop processing this file if there are no gaps
    if (gaps.isEmpty()) {
      processedScans += dataFile.getNumOfScans();
    }

    // Get all scans of this data file
    Scan scanNumbers[] = dataFile.getScanNumbers(msLevel).toArray(Scan[]::new);
    if (scanNumbers == null) {
      logger.log(Level.WARNING, "Could not read file with the MS level of " + msLevel);
      setStatus(TaskStatus.ERROR);
      return;
    }

    // Process each scan
    for (Scan scan : scanNumbers) {

      // Canceled?
      if (isCanceled()) {
        return;
      }

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

    // Append processed feature list to the project
    project.addFeatureList(processedPeakList);

    // Add quality parameters to peaks
    //QualityParameters.calculateQualityParameters(processedPeakList);

    dataFile.getAppliedMethods().forEach(m -> processedPeakList.getAppliedMethods().add(m));
    // Add task description to peakList
    processedPeakList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Targeted feature detection ",
            TargetedFeatureDetectionModule.class, parameters, getModuleCallDate()));

    logger.log(Level.INFO, "Finished targeted feature detection on {0}", this.dataFile);
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
          // Removing the FEFF character is important in case the CSV
          // file contains
          // byte-order-mark
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
    return "Targeted feature detection " + this.dataFile;
  }
}
