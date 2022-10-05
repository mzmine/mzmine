/*
 *  Copyright 2006-2022 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */
package io.github.mzmine.modules.dataprocessing.featdet_targeted;

import com.Ostermiller.util.CSVParser;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.Gap;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.io.FileReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

  TargetedPeakDetectionModuleTask(MZmineProject project, ParameterSet parameters,
      RawDataFile dataFile, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.parameters = parameters;

    suffix = parameters.getParameter(TargetedPeakDetectionParameters.suffix).getValue();
    msLevel = parameters.getParameter(TargetedPeakDetectionParameters.msLevel).getValue();
    peakListFile = parameters.getParameter(TargetedPeakDetectionParameters.peakListFile).getValue();
    fieldSeparator = parameters.getParameter(TargetedPeakDetectionParameters.fieldSeparator)
        .getValue();
    ignoreFirstLine = parameters.getParameter(TargetedPeakDetectionParameters.ignoreFirstLine)
        .getValue();

    intTolerance = parameters.getParameter(TargetedPeakDetectionParameters.intTolerance).getValue();
    mzTolerance = parameters.getParameter(TargetedPeakDetectionParameters.MZTolerance).getValue();
    rtTolerance = parameters.getParameter(TargetedPeakDetectionParameters.RTTolerance).getValue();

    this.dataFile = dataFile;
  }

  public void run() {

    setStatus(TaskStatus.PROCESSING);

    // Calculate total number of scans in all files
    totalScans = dataFile.getNumOfScans(msLevel);

    // Create new feature list
    processedPeakList = new ModularFeatureList(dataFile.getName() + " " + suffix,
        getMemoryMapStorage(), dataFile);

    List<PeakInformation> peaks = this.readFile();

    if (peaks == null || peaks.isEmpty()) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not read file or the file is empty ");
      return;
    }

    final Map<FeatureListRow, Gap> rowGapMap = new HashMap<>();
    List<io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.Gap> gaps = new ArrayList<>();
    // Fill new feature list with empty rows
    for (int row = 0; row < peaks.size(); row++) {
      FeatureListRow newRow = new ModularFeatureListRow((ModularFeatureList) processedPeakList,
          ID++);

      Range<Double> mzRange = mzTolerance.getToleranceRange(peaks.get(row).getMZ());
      Range<Float> rtRange = rtTolerance.getToleranceRange((float) peaks.get(row).getRT());
      newRow.addCompoundAnnotation(peaks.get(row).toCompountDBAnnotation());

      io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.Gap newGap = new io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.Gap(
          newRow, dataFile, mzRange, rtRange, intTolerance);
      gaps.add(newGap);

      rowGapMap.put(newRow, newGap);
    }

    // Canceled?
    if (isCanceled()) {
      return;
    }

    // Get all scans of this data file
    final ScanDataAccess access = EfficientDataAccess.of(dataFile, ScanDataType.CENTROID,
        new ScanSelection(msLevel));

    while (access.hasNextScan()) {
      access.nextScan();
      // Canceled?
      if (isCanceled()) {
        return;
      }

      // Feed this scan to all gaps
      for (io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.Gap gap : gaps) {
        gap.offerNextScan(access);
      }

      processedScans++;
    }

    // Finalize gaps
    for (Entry<FeatureListRow, Gap> entry : rowGapMap.entrySet()) {
      final FeatureListRow row = entry.getKey();
      final Gap gap = entry.getValue();
      if (gap.noMoreOffers()) {
        processedPeakList.addRow(row);
      }
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
    try(final FileReader dbFileReader = new FileReader(peakListFile)) {
      List<PeakInformation> list = new ArrayList<>();

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
