/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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
/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 * 
 * It is freely available under the GNU GPL licence of MZmine2.
 * 
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 * 
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package net.sf.mzmine.modules.peaklistmethods.io.gnpsexport;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.collect.Range;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.*;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.io.gnpsexport.GNPSExportAndSubmitParameters.RowFilter;
import net.sf.mzmine.modules.peaklistmethods.io.siriusexport.MergeUtils;
import net.sf.mzmine.modules.peaklistmethods.io.siriusexport.SiriusExportParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.files.FileAndPathUtil;

/**
 * Exports all files needed for GNPS
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class GNPSmgfExportTask extends AbstractTask {
  // Logger.
  private final Logger LOG = Logger.getLogger(getClass().getName());

  //
  private final PeakList[] peakLists;
  private final File fileName;
  private final String plNamePattern = "{}";
  private int currentIndex = 0;
  private final String massListName;

  // by robin
  private NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
  private NumberFormat intensityForm = MZmineCore.getConfiguration().getIntensityFormat();
  // seconds
  private NumberFormat rtsForm = new DecimalFormat("0.###");
  // correlation
  private NumberFormat corrForm = new DecimalFormat("0.0000");

  private RowFilter filter;

  // by kaidu
  private MergeUtils mergeUtils;
  private SiriusExportParameters.MERGE_MODE mergeMode;

  GNPSmgfExportTask(ParameterSet parameters) {
    this.peakLists =
        parameters.getParameter(GNPSExportAndSubmitParameters.PEAK_LISTS).getValue().getMatchingPeakLists();

    this.fileName = parameters.getParameter(GNPSExportAndSubmitParameters.FILENAME).getValue();
    this.massListName = parameters.getParameter(GNPSExportAndSubmitParameters.MASS_LIST).getValue();
    this.filter = parameters.getParameter(GNPSExportAndSubmitParameters.FILTER).getValue();
    this.mergeUtils = new MergeUtils();
    mergeUtils.setPeakRemovalThreshold(parameters.getParameter(GNPSExportAndSubmitParameters.PEAK_COUNT_PARAMETER).getValue());
    mergeUtils.setCosineThreshold(parameters.getParameter(GNPSExportAndSubmitParameters.COSINE_PARAMETER).getValue());
    mergeUtils.setExpectedPPM(parameters.getParameter(GNPSExportAndSubmitParameters.MASS_ACCURACY).getValue());
    mergeUtils.setMergeMasses(parameters.getParameter(GNPSExportAndSubmitParameters.AVERAGE_OVER_MASS).getValue());
    final double offset = parameters.getParameter(GNPSExportAndSubmitParameters.isolationWindowOffset).getValue();
    final double width = parameters.getParameter(GNPSExportAndSubmitParameters.isolationWindowWidth).getValue();
    mergeUtils.setIsolationWindow(Range.closed(offset-width, offset+width));
    mergeMode = parameters.getParameter(GNPSExportAndSubmitParameters.MERGE).getValue();
  }

  @Override
  public double getFinishedPercentage() {
    if (peakLists.length == 0)
      return 1;
    else
      return currentIndex / peakLists.length;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // Shall export several files?
    boolean substitute = fileName.getPath().contains(plNamePattern);

    // Process peak lists
    for (PeakList peakList : peakLists) {
      currentIndex++;

      // Filename
      File curFile = fileName;
      if (substitute) {
        // Cleanup from illegal filename characters
        String cleanPlName = peakList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        // Substitute
        String newFilename =
            fileName.getPath().replaceAll(Pattern.quote(plNamePattern), cleanPlName);
        curFile = new File(newFilename);
      }
      curFile = FileAndPathUtil.getRealFilePath(curFile, "mgf");

      // Open file
      FileWriter writer;
      try {
        writer = new FileWriter(curFile);
      } catch (Exception e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not open file " + curFile + " for writing.");
        return;
      }

      try {
        export(peakList, writer, curFile);
      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Error while writing into file " + curFile + ": " + e.getMessage());
        return;
      }

      // Cancel?
      if (isCanceled()) {
        return;
      }

      // Close file
      try {
        writer.close();
      } catch (Exception e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not close file " + curFile);
        return;
      }

      // If peak list substitution pattern wasn't found,
      // treat one peak list only
      if (!substitute)
        break;
    }

    if (getStatus() == TaskStatus.PROCESSING)
      setStatus(TaskStatus.FINISHED);
  }

  private int export(PeakList peakList, FileWriter writer, File curFile) throws IOException {
    final ErrorStatistics stats = new ErrorStatistics();
    HashMap<String, int[]> fragmentScansNumbers = getFragmentScansNumbers(peakList.getRawDataFiles());
    // count exported
    int count = 0;
    int countMissingMassList = 0;
    for (PeakListRow row : peakList.getRows()) {
      // do not export if no MSMS
      if (!filter.filter(row))
        continue;

      // Get the MS/MS scan number
      Feature bestPeak = row.getBestPeak();
      if (bestPeak == null)
        continue;


      if (mergeMode== SiriusExportParameters.MERGE_MODE.NO_MERGE) {
        gnpsOutputNoMerge(row, bestPeak, writer, stats);
      } else {
        gnpsOutputMerge(row, writer, stats,fragmentScansNumbers);
      }

    }
    if (stats.count == 0)
      if (stats.missingMassList > 0)
        throw new MSDKRuntimeException("No MS/MS scans exported: " + countMissingMassList
            + " scans have no mass list " + massListName);
      else
        throw new MSDKRuntimeException("No MS/MS scans exported.");

    LOG.info(
        MessageFormat.format("Total of {0} feature rows (MS/MS mass lists) were exported ({1})",
            count, peakList.getName()));
    if (countMissingMassList > 0)
      LOG.warning(MessageFormat.format(
          "WARNING: Total of {0} feature rows have an MS/MS scan but NO mass list (this shouldn't be a problem if a scan filter was applied in the mass detection step) ({1})",
          countMissingMassList, peakList.getName()));

    return count;
  }

  private void gnpsOutputMerge(PeakListRow row, FileWriter writer, ErrorStatistics stats, HashMap<String,int[]> ms2ScanNumbers) throws IOException {
    final MergeUtils.MergedSpectrum ms2;
    if (mergeMode == SiriusExportParameters.MERGE_MODE.MERGE_CONSECUTIVE_SCANS) {
      ms2 = mergeUtils.mergeConsecutiveScans(row,row.getBestPeak());
    } else {
      ms2 = mergeUtils.mergeScansFromDifferentOrigins(row);
    }
    double retTimeInSeconds = ((row.getAverageRT() * 60 * 100.0) / 100.);
    final String newLine = System.lineSeparator();
    PeakListRow copyRow = copyPeakRow(row);
    // Best peak always exists, because peak list row has at least one peak
    Feature bestPeak = copyRow.getBestPeak();

    // Get the heighest peak with a MS/MS scan number (with mass list)
    boolean missingMassList = false;
    writer.write("BEGIN IONS" + newLine);

    writer.write("FEATURE_ID=" + row.getID() + newLine);

    String mass = mzForm.format(row.getAverageMZ());
    if (mass != null)
        writer.write("PEPMASS=" + mass + newLine);

      writer.write("SCANS=" + row.getID() + newLine);
      writer.write("RTINSECONDS=" + rtsForm.format(retTimeInSeconds) + newLine);

      int msmsCharge = ms2.precursorCharge;
      String msmsPolarity = ms2.polarity.asSingleChar();
      if (msmsPolarity.equals("0"))
        msmsPolarity = "";
      if (msmsCharge == 0) {
        msmsCharge = 1;
        msmsPolarity = "";
      }
      writer.write("CHARGE=" + msmsCharge + msmsPolarity + newLine);

    writer.write("MERGED_STATS=");
    writer.write(String.valueOf(ms2.scanIds.length));
    writer.write(" / ");
    writer.write(String.valueOf(ms2.totalNumberOfScans()));
    writer.write(" (");
    writer.write(String.valueOf(ms2.removedScansByLowQuality));
    writer.write(" removed due to low quality, ");
    writer.write(String.valueOf(ms2.removedScansByLowCosine));
    writer.write(" removed due to low cosine).");
    writer.write(newLine);

      writer.write("MSLEVEL=2" + newLine);

      MergeUtils.MergeDataPoint peaks[] = ms2.data;
      for (MergeUtils.MergeDataPoint peak : peaks) {
        writer.write(mzForm.format(peak.getMZ()) + " " + intensityForm.format(peak.getSumIntensity())
                + newLine);
      }

      writer.write("END IONS" + newLine);
      writer.write(newLine);
      stats.count++;

  }

  private HashMap<String, int[]> getFragmentScansNumbers(RawDataFile[] rawDataFiles) {
    final HashMap<String, int[]> fragmentScans = new HashMap<>();
    for (RawDataFile r : rawDataFiles) {
      int[] scans = new int[0];
      for (int msLevel : r.getMSLevels()) {
        if (msLevel > 1) {
          int[] concat = r.getScanNumbers(msLevel);
          int offset = scans.length;
          scans = Arrays.copyOf(scans, scans.length + concat.length);
          System.arraycopy(concat, 0, scans, offset, concat.length);
        }
      }
      Arrays.sort(scans);
      fragmentScans.put(r.getName(), scans);
    }
    return fragmentScans;
  }

  protected static class ErrorStatistics {
    protected int missingMassList = 0;
    protected int count = 0;
  }

  private void gnpsOutputNoMerge(PeakListRow row, Feature bestPeak, FileWriter writer, ErrorStatistics stats) throws IOException {
    double retTimeInSeconds = ((row.getAverageRT() * 60 * 100.0) / 100.);
    final String newLine = System.lineSeparator();
    int msmsScanNumber = bestPeak.getMostIntenseFragmentScanNumber();
      PeakListRow copyRow = copyPeakRow(row);
      // Best peak always exists, because peak list row has at least one peak
      bestPeak = copyRow.getBestPeak();

      // Get the heighest peak with a MS/MS scan number (with mass list)
      boolean missingMassList = false;
      msmsScanNumber = bestPeak.getMostIntenseFragmentScanNumber();
      while (msmsScanNumber < 1
              || getScan(bestPeak, msmsScanNumber).getMassList(massListName) == null) {
        // missing masslist
        if (msmsScanNumber > 0)
          missingMassList = true;

        copyRow.removePeak(bestPeak.getDataFile());
        if (copyRow.getPeaks().length == 0)
          break;

        bestPeak = copyRow.getBestPeak();
        msmsScanNumber = bestPeak.getMostIntenseFragmentScanNumber();
      }
      if (missingMassList)
        stats.missingMassList++;

    if (msmsScanNumber >= 1) {
      // MS/MS scan must exist, because msmsScanNumber was > 0
      Scan msmsScan = bestPeak.getDataFile().getScan(msmsScanNumber);

      MassList massList = msmsScan.getMassList(massListName);


      if (massList == null) {
        return;
      }

      writer.write("BEGIN IONS" + newLine);

      writer.write("FEATURE_ID=" + row.getID() + newLine);

      String mass = mzForm.format(row.getAverageMZ());
      if (mass != null)
        writer.write("PEPMASS=" + mass + newLine);

      writer.write("SCANS=" + row.getID() + newLine);
      writer.write("RTINSECONDS=" + rtsForm.format(retTimeInSeconds) + newLine);

      int msmsCharge = msmsScan.getPrecursorCharge();
      String msmsPolarity = msmsScan.getPolarity().asSingleChar();
      if (msmsPolarity.equals("0"))
        msmsPolarity = "";
      if (msmsCharge == 0) {
        msmsCharge = 1;
        msmsPolarity = "";
      }
      writer.write("CHARGE=" + msmsCharge + msmsPolarity + newLine);

      writer.write("MSLEVEL=2" + newLine);

      DataPoint peaks[] = massList.getDataPoints();
      for (DataPoint peak : peaks) {
        writer.write(mzForm.format(peak.getMZ()) + " " + intensityForm.format(peak.getIntensity())
                + newLine);
      }

      writer.write("END IONS" + newLine);
      writer.write(newLine);
      stats.count++;
    }
  }

  public Scan getScan(Feature f, int msmsscan) {
    return f.getDataFile().getScan(msmsscan);
  }

  @Override
  public String getTaskDescription() {
    return "Exporting GNPS of peak list(s) " + Arrays.toString(peakLists) + " to MGF file(s)";
  }

  /**
   * Create a copy of a peak list row.
   */
  private static PeakListRow copyPeakRow(final PeakListRow row) {
    // Copy the peak list row.
    final PeakListRow newRow = new SimplePeakListRow(row.getID());
    PeakUtils.copyPeakListRowProperties(row, newRow);

    // Copy the peaks.
    for (final Feature peak : row.getPeaks()) {


      final Feature newPeak = new SimpleFeature(peak);
      PeakUtils.copyPeakProperties(peak, newPeak);
      newRow.addPeak(peak.getDataFile(), newPeak);

    }

    return newRow;
  }

}
