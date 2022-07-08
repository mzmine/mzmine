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
package io.github.mzmine.modules.dataprocessing.featdet_dfbuilder;

import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.impl.MSnInfoImpl;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.Ostermiller.util.CSVParser;
import com.google.common.collect.Range;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.ScanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DiagnosticFilterTask extends AbstractTask {

  private final MZmineProject project;
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  // User Parameters
  private final ParameterSet parameters;
  private final ScanSelection scanSelection;
  private final File diagnosticFile;
  private final Boolean useExclusion;
  private final File exclusionFile;
  private final MZTolerance mzDifference;
  private final Boolean exportFile;
  private final File fileName;
  private final Double basePeakPercent;
  private final RTTolerance rtTolerance;
  private final RawDataFile rawDataFile;

  private final ModularFeatureList targetPeakList;

  private int totalScans, processedScans;
  private int finishedLines = 0;
  private int rowID = 1;

  public DiagnosticFilterTask(MZmineProject project, RawDataFile dataFile, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.parameters = parameters;
    this.rawDataFile = dataFile;

    this.scanSelection = parameters.getParameter(DiagnosticFilterParameters.scanSelection)
        .getValue();
    this.diagnosticFile = parameters.getParameter(DiagnosticFilterParameters.diagnosticFile)
        .getValue();
    this.useExclusion = parameters.getParameter(DiagnosticFilterParameters.exclusionFile)
        .getValue();
    this.exclusionFile = !useExclusion ? null
        : parameters.getParameter(DiagnosticFilterParameters.exclusionFile).getEmbeddedParameter()
            .getValue();
    this.mzDifference = parameters.getParameter(DiagnosticFilterParameters.mzDifference).getValue();
    this.basePeakPercent =
        parameters.getParameter(DiagnosticFilterParameters.basePeakPercent).getValue() / 100;
    this.rtTolerance = parameters.getParameter(DiagnosticFilterParameters.rtTolerance).getValue();
    this.exportFile = parameters.getParameter(DiagnosticFilterParameters.exportFile).getValue();
    this.fileName = !exportFile ? null
        : parameters.getParameter(DiagnosticFilterParameters.exportFile).getEmbeddedParameter()
            .getValue();
    this.targetPeakList = new ModularFeatureList(rawDataFile.getName() + " targetChromatograms",
        getMemoryMapStorage(), rawDataFile);
  }

  public void run() {

    setStatus(TaskStatus.PROCESSING);

    logger.info("Started diagnostic filtering on " + rawDataFile);

    ScanDataAccess scans = EfficientDataAccess.of(rawDataFile, ScanDataType.CENTROID, scanSelection);
    totalScans = scans.getNumberOfScans();

    List<DiagnosticInformation> targetList = this.readDiagnostic();
    List<ExclusionInformation> exclusionList = !useExclusion ? null : this.readExclusion();

    // ExportList that will contain output m/z values, RT, and scan number for
    // ID for export to a CSV. Not used if not selected.
    List<String> exportList = new ArrayList<>();

    // Error catch - no scans in selection.
    if(totalScans == 0){
      setStatus(TaskStatus.ERROR);
      final String msg = "No scans detected in selection range for " + rawDataFile.getName();
      setErrorMessage(msg);
      return;
    }

    // Error catch - no diagnostic filters in import.
    if(targetList.size() == 0){
      setStatus(TaskStatus.ERROR);
      final String msg = "No diagnostic filters detected in "+ diagnosticFile;
      setErrorMessage(msg);
      return;
    }

    // Error catch - Fragmentation filtering cannot be done on full scan.
    if(scanSelection.getMsLevel() != null){
      if(scanSelection.getMsLevel() == 1){
        setStatus(TaskStatus.ERROR);
        final String msg = "MS Level cannot be 1. Please update Scan Selection Field.";
        setErrorMessage(msg);
        return;
      }
    }

    /*
      Loop through each scan in selection. Search MSn scans for neutral losses and/or product ions
      specified in target list. If all specified fragmentation detected, build chromatogram for
      full scan precursor ion in specified MZ and RT ranges.
     */
    while(scans.hasNextScan()){

      scans.nextScan();
      Scan scan = scans.getCurrentScan();
      assert scan != null;

      // Canceled?
      if (isCanceled()) {
        return;
      }

      /*
        Get full scan precursor mass. If MS Level > 2, get from associated MS2 scan.
        If MS Level unspecifed, skip full scans.
       */
      double precursorMZ;
      if (scan.getMSLevel() == 2) {
        precursorMZ = scan.getPrecursorMz();
      } else if (scan.getMsMsInfo() instanceof MSnInfoImpl msn) {
        precursorMZ = msn.getMS2PrecursorMz();
      } else {
        processedScans++;
        continue;
      }

      // Check if scan precursor MZ and RT in exclusion list.
      if (useExclusion) {

        boolean exclude = false;

        for (ExclusionInformation target : exclusionList) {
          Double mz = target.getMZ();
          Range<Double> rtRange = target.getRTRange();
          if (mzDifference.checkWithinTolerance(mz, precursorMZ)) {
            if (rtRange.contains((double) scan.getRetentionTime())) {
              exclude = true;
            }
          }
        }
        if (exclude) {
          processedScans++;
          continue;
        }
      }

      // Get noise threshold relative to scan base peak intensity.
      double noiseThreshold;
      noiseThreshold = scan.getBasePeakIntensity() * basePeakPercent;

      // Get data point of scan. Keep only those above threshold.
      // TODO - evaluate efficient methods for scan value access.
      DataPoint[] dp = ScanUtils.extractDataPoints(scan);
      dp = ScanUtils.getFiltered(dp, noiseThreshold);

      // If no data points meet threshold, skip scan
      if (dp.length == 0) {
        processedScans++;
        continue;
      }

      // Get Product Ions
      List<Double> fragmentIon = new ArrayList<>();
      for (DataPoint dataPoint : dp) {
        fragmentIon.add(dataPoint.getMZ());
      }

      // Target information
      HashMap<String, Boolean> targetMap = new HashMap<>();

      /*
        Search scan data points for all neutral loss and product ions. Determine if all
        combinations are found for each target.
       */
      for (DiagnosticInformation target : targetList) {

        String targetName = target.getName();
        double[] targetedMZ = target.getMZList();
        double[] targetedNF = target.getNFList();

        List<Boolean> foundMZ = new ArrayList<>();
        List<Boolean> foundNF = new ArrayList<>();

        // Check if fragment ion in scan
        // If no fragment ions to be searched, return true
        if (targetedMZ[0] != 0) {
          for (double key : targetedMZ) {

            List<Boolean> check = new ArrayList<>();
            for (double MZ : fragmentIon) {

              check.add(mzDifference.getToleranceRange(key).contains(MZ));
            }
            foundMZ.add(check.contains(true));
          }
        } else {
          foundMZ.add(true);
        }

        // Check if neutral loss in scan
        // If no neutral loss to be searched, return true
        if (targetedNF[0] != 0) {
          for (double key : targetedNF) {
            List<Boolean> check = new ArrayList<>();

            // Neutral loss calculated from scan precursors. Important for MSn > 2 scans.
            double targetNL = scan.getPrecursorMz() - key;

            for (double MZ : fragmentIon) {

              check.add(mzDifference.getToleranceRange(targetNL).contains(MZ));
            }
            foundNF.add(check.contains(true));
          }
        } else {
          foundNF.add(true);
        }

        // If all fragment ions and neutral losses found, add target
        if (!foundMZ.contains(false) && !foundNF.contains(false)) {
          targetMap.put(targetName, true);
        } else {
          targetMap.put(targetName, false);
        }
      }

      // If target found, build chromatogram in RT range
      if (targetMap.containsValue(true)) {

        // Get target info
        Range<Double> mzRange = mzDifference.getToleranceRange(precursorMZ);
        Range<Float> rtRange = rtTolerance.getToleranceRange(scan.getRetentionTime());
        StringBuilder comment = new StringBuilder();

        // Format all diagnostic targets names detected.
        for (Map.Entry<String, Boolean> entry : targetMap.entrySet()) {
          if (entry.getValue()) {
            comment.append("target=").append(entry.getKey()).append(';');
          }
        }

        // Remove tailing semi-colon
        comment = new StringBuilder(comment.substring(0, comment.length() - 1));

        // Build simple feature for precursor in ranges.
        ModularFeature newFeature = FeatureUtils
            .buildSimpleModularFeature(targetPeakList, rawDataFile, rtRange, mzRange);

        // Add feature to feature list.
        if (newFeature != null) {

          ModularFeatureListRow newFeatureListRow = new ModularFeatureListRow(targetPeakList,
              rowID, newFeature);

          newFeatureListRow.setComment(comment.toString());

          targetPeakList.addRow(newFeatureListRow);
          rowID++;
        }

        if (exportFile) {

          // Add scan MZ, RT, detected targets, scan number, and raw data file name to export.
          String dataMZ = Double.toString(scan.getPrecursorMz());
          String dataRT = Double.toString(scan.getRetentionTime());

          StringBuilder temp = new StringBuilder(dataMZ + "," + dataRT + ",");

          for (Map.Entry<String, Boolean> entry : targetMap.entrySet()) {
            if (entry.getValue()) {
              temp.append("target=").append(entry.getKey()).append(';');
            }
          }
          temp = new StringBuilder(temp.substring(0, temp.length() - 1));

          temp.append(",").append(scan.getScanNumber()).append(',').append(rawDataFile.getName());

          exportList.add(temp.toString());
        }
      }

      processedScans++;
    }

    // No MSn features detected in range.
    if (targetPeakList.isEmpty()) {
      setStatus(TaskStatus.ERROR);
      final String msg =
          "No MSn precursor features detected in selected range for " + rawDataFile.getName();
      setErrorMessage(msg);
      return;
    }

    if (exportFile) {
      writeDiagnostic(exportList);
    }

    // sort and reset IDs here to ahve the same sorting for every feature list
    FeatureListUtils.sortByDefaultRT(targetPeakList, true);

    // Explicitly get scans for full scan (msLevel = 1).
    List<Scan> scan;
    if(scanSelection.getScanRTRange() == null){
      scan = rawDataFile.getScanNumbers(1);
    } else {
      scan = Arrays.asList(rawDataFile.getScanNumbers(1, scanSelection.getScanRTRange()));
    }

    targetPeakList.setSelectedScans(rawDataFile, scan);

    rawDataFile.getAppliedMethods().forEach(m -> targetPeakList.getAppliedMethods().add(m));
    // Add new feature list to the project
    targetPeakList.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(ModularADAPChromatogramBuilderModule.class, parameters,
            getModuleCallDate()));
    project.addFeatureList(targetPeakList);

    logger.log(Level.INFO, "Finished diagnostic fragmentation screening on {0}", this.rawDataFile);
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesKey(int)
   */
  public Comparable<Integer> getSeriesKey(int series) {
    return series;
  }

  public void cancel() {
    setStatus(TaskStatus.CANCELED);
  }

  public double getFinishedPercentage() {
    if (totalScans == 0) {
      return 0;
    } else {
      return ((double) processedScans / totalScans);
    }
  }

  public String getTaskDescription() {
    return "Screening for fragment patterns in " + rawDataFile;
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.NORMAL;
  }

  /**
   * Read CSV file containing diagnostic targets to be searched for.
   * File is 3 column CSV file containing the target name, product ion masses, and neutral loss
   * masses with no headers. If multiple masses specified for product ion or neutral loss fields
   * they are separated by a semi-colon (';').
   */
  public List<DiagnosticInformation> readDiagnostic() {

    FileReader dbFileReader;
    try {
      List<DiagnosticInformation> list = new ArrayList<>();
      dbFileReader = new FileReader(diagnosticFile);

      String[][] diagnosticListValue = CSVParser.parse(dbFileReader, ',');

      for (; finishedLines < diagnosticListValue.length; finishedLines++) {
        try {

          String name;
          String mzString;
          String nlString = "";

          name = diagnosticListValue[finishedLines][0].trim();

          // Remove FEFF character from CSV
          mzString = diagnosticListValue[finishedLines][1].replace("\uFEFF", "").trim();

          // If no neutral loss specified, file will not have a third column to parse.
          try {
            nlString = diagnosticListValue[finishedLines][2].replace("\uFEFF", "").trim();
          } catch (Exception e) {
            e.printStackTrace();
          }

          double[] mz = {0};
          double[] nl = {0};

          if (!mzString.isEmpty()) {
            mz = Stream.of(mzString.split(";")).mapToDouble(Double::parseDouble).toArray();
          }

          if (!nlString.isEmpty()) {
            nl = Stream.of(nlString.split(";")).mapToDouble(Double::parseDouble).toArray();
          }

          list.add(new DiagnosticInformation(name, mz, nl));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      dbFileReader.close();
      return list;

    } catch (Exception e) {
      e.printStackTrace();
      logger.log(Level.WARNING, "Could not read file " + diagnosticFile, e);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(e.toString());
      return null;
    }

  }

  /**
   * Read CSV file containing ions to exclude from the run. File is a 3 column table with no
   * headers. The first column contains the MZ values, The second the start RT for exclusion.
   * The third the end RT for exclusion.
   */
  public List<ExclusionInformation> readExclusion() {

    FileReader dbFileReader;
    try {
      List<ExclusionInformation> list = new ArrayList<>();
      dbFileReader = new FileReader(exclusionFile);

      String[][] exclusionListValue = CSVParser.parse(dbFileReader, ',');

      for (; finishedLines < exclusionListValue.length; finishedLines++) {
        try {

          // Remove FEFF character from CSV
          String mzString = exclusionListValue[finishedLines][0].replace("\uFEFF", "").trim();
          String rtStart = exclusionListValue[finishedLines][1].replace("\uFEFF", "").trim();
          String rtEnd = exclusionListValue[finishedLines][2].replace("\uFEFF", "").trim();

          Double mz = Double.parseDouble(mzString);
          Range<Double> rtRange = Range
              .closed(Double.parseDouble(rtStart), Double.parseDouble(rtEnd));

          list.add(new ExclusionInformation(mz, rtRange));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      dbFileReader.close();
      return list;
    } catch (Exception e) {
      e.printStackTrace();
      logger.log(Level.WARNING, "Could not read file " + diagnosticFile, e);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(e.toString());
      return null;
    }

  }

  /**
   * Write scan information for detected diagnostic filters.
   * @param export String array containing scan info
   */
  public void writeDiagnostic(List<String> export) {
    // Write output to csv file - for DFBuilder target chromatogram builder module.
    try {
      // Cancel?
      if (isCanceled()) {
        return;
      }

      String namePattern = "{}";
      File curFile = fileName;

      if (fileName.getPath().contains(namePattern)) {

        String cleanPlName = rawDataFile.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        // Substitute
        String newFilename = fileName.getPath().replaceAll(Pattern.quote(namePattern), cleanPlName);
        curFile = new File(newFilename);
      }

      FileWriter writer = new FileWriter(curFile, true);

      String collect = String.join("\n", export);

      writer.write(collect);
      writer.write("\n");
      writer.close();

    } catch (IOException e) {
      System.out.print("Could not output to file");
      System.out.print(Arrays.toString(e.getStackTrace()));

      setStatus(TaskStatus.FINISHED);
    }
  }
}
