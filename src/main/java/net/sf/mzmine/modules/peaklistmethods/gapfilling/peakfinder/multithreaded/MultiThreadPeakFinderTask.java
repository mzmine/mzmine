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

package net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfinder.multithreaded;

import java.util.Vector;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfinder.Gap;
import net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfinder.RegressionInfo;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

class MultiThreadPeakFinderTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private PeakList peakList, processedPeakList;
  private String suffix;
  private double intTolerance;
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  private boolean rtCorrection;
  private ParameterSet parameters;
  private int processedScans, totalScans;
  private boolean MASTERLIST = true, removeOriginal;
  private int masterSample = 0;

  // start and end (exclusive) for raw data file processing
  private int start;
  private int endexcl;

  MultiThreadPeakFinderTask(MZmineProject project, PeakList peakList, ParameterSet parameters,
      int start, int endexcl) {

    this.project = project;
    this.peakList = peakList;
    this.parameters = parameters;

    suffix = parameters.getParameter(MultiThreadPeakFinderParameters.suffix).getValue();
    intTolerance = parameters.getParameter(MultiThreadPeakFinderParameters.intTolerance).getValue();
    mzTolerance = parameters.getParameter(MultiThreadPeakFinderParameters.MZTolerance).getValue();
    rtTolerance = parameters.getParameter(MultiThreadPeakFinderParameters.RTTolerance).getValue();
    rtCorrection = parameters.getParameter(MultiThreadPeakFinderParameters.RTCorrection).getValue();
    removeOriginal = parameters.getParameter(MultiThreadPeakFinderParameters.autoRemove).getValue();

    this.start = start;
    this.endexcl = endexcl;
  }

  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("Running gap filler on " + peakList);

    // Calculate total number of scans in all files
    for (RawDataFile dataFile : peakList.getRawDataFiles()) {
      totalScans += dataFile.getNumOfScans(1);
    }

    // Create new peak list
    processedPeakList = new SimplePeakList(peakList + " " + suffix, peakList.getRawDataFiles());

    // Fill new peak list with empty rows
    for (int row = 0; row < peakList.getNumberOfRows(); row++) {
      PeakListRow sourceRow = peakList.getRow(row);
      PeakListRow newRow = new SimplePeakListRow(sourceRow.getID());
      newRow.setComment(sourceRow.getComment());
      for (PeakIdentity ident : sourceRow.getPeakIdentities()) {
        newRow.addPeakIdentity(ident, false);
      }
      if (sourceRow.getPreferredPeakIdentity() != null) {
        newRow.setPreferredPeakIdentity(sourceRow.getPreferredPeakIdentity());
      }
      processedPeakList.addRow(newRow);
    }

    if (rtCorrection) {
      totalScans *= 2;
      // Fill the gaps of a random sample using all the other samples and
      // take it as master list
      // to fill the gaps of the other samples
      masterSample = (int) Math.floor(Math.random() * peakList.getNumberOfRawDataFiles());
      fillList(MASTERLIST);

      // Process all raw data files
      fillList(!MASTERLIST);

    } else {

      // Process all raw data files
      for (RawDataFile dataFile : peakList.getRawDataFiles()) {

        // Canceled?
        if (isCanceled()) {
          return;
        }

        Vector<Gap> gaps = new Vector<Gap>();

        // Fill each row of this raw data file column, create new empty
        // gaps
        // if necessary
        for (int row = 0; row < peakList.getNumberOfRows(); row++) {
          PeakListRow sourceRow = peakList.getRow(row);
          PeakListRow newRow = processedPeakList.getRow(row);

          Feature sourcePeak = sourceRow.getPeak(dataFile);

          if (sourcePeak == null) {

            // Create a new gap

            Range<Double> mzRange = mzTolerance.getToleranceRange(sourceRow.getAverageMZ());
            Range<Double> rtRange = rtTolerance.getToleranceRange(sourceRow.getAverageRT());

            Gap newGap = new Gap(newRow, dataFile, mzRange, rtRange, intTolerance);

            gaps.add(newGap);

          } else {
            newRow.addPeak(dataFile, sourcePeak);
          }

        }

        // Stop processing this file if there are no gaps
        if (gaps.size() == 0) {
          processedScans += dataFile.getNumOfScans();
          continue;
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

      }
    }

    // Append processed peak list to the project
    project.addPeakList(processedPeakList);

    // Add quality parameters to peaks
    QualityParameters.calculateQualityParameters(processedPeakList);

    // Add task description to peakList
    processedPeakList
        .addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod("Gap filling ", parameters));

    // Remove the original peaklist if requested
    if (removeOriginal)
      project.removePeakList(peakList);

    logger.info("Finished gap-filling on " + peakList);
    setStatus(TaskStatus.FINISHED);

  }

  public void fillList(boolean masterList) {
    for (int i = 0; i < peakList.getNumberOfRawDataFiles(); i++) {
      if (i != masterSample) {

        RawDataFile datafile1;
        RawDataFile datafile2;

        if (masterList) {
          datafile1 = peakList.getRawDataFile(masterSample);
          datafile2 = peakList.getRawDataFile(i);
        } else {
          datafile1 = peakList.getRawDataFile(i);
          datafile2 = peakList.getRawDataFile(masterSample);
        }
        RegressionInfo info = new RegressionInfo();

        for (PeakListRow row : peakList.getRows()) {
          Feature peaki = row.getPeak(datafile1);
          Feature peake = row.getPeak(datafile2);
          if (peaki != null && peake != null) {
            info.addData(peake.getRT(), peaki.getRT());
          }
        }

        info.setFunction();

        // Canceled?
        if (isCanceled()) {
          return;
        }

        Vector<Gap> gaps = new Vector<Gap>();

        // Fill each row of this raw data file column, create new empty
        // gaps
        // if necessary
        for (int row = 0; row < peakList.getNumberOfRows(); row++) {
          PeakListRow sourceRow = peakList.getRow(row);
          PeakListRow newRow = processedPeakList.getRow(row);

          Feature sourcePeak = sourceRow.getPeak(datafile1);

          if (sourcePeak == null) {

            // Create a new gap

            double mz = sourceRow.getAverageMZ();
            double rt2 = -1;
            if (!masterList) {
              if (processedPeakList.getRow(row).getPeak(datafile2) != null) {
                rt2 = processedPeakList.getRow(row).getPeak(datafile2).getRT();
              }
            } else {
              if (peakList.getRow(row).getPeak(datafile2) != null) {
                rt2 = peakList.getRow(row).getPeak(datafile2).getRT();
              }
            }

            if (rt2 > -1) {

              double rt = info.predict(rt2);

              if (rt != -1) {

                Range<Double> mzRange = mzTolerance.getToleranceRange(mz);
                Range<Double> rtRange = rtTolerance.getToleranceRange(rt);

                Gap newGap = new Gap(newRow, datafile1, mzRange, rtRange, intTolerance);

                gaps.add(newGap);
              }
            }

          } else {
            newRow.addPeak(datafile1, sourcePeak);
          }

        }

        // Stop processing this file if there are no gaps
        if (gaps.size() == 0) {
          processedScans += datafile1.getNumOfScans();
          continue;
        }

        // Get all scans of this data file
        int scanNumbers[] = datafile1.getScanNumbers(1);

        // Process each scan
        for (int scanNumber : scanNumbers) {

          // Canceled?
          if (isCanceled()) {
            return;
          }

          // Get the scan
          Scan scan = datafile1.getScan(scanNumber);

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
      }
    }
  }

  public double getFinishedPercentage() {
    if (totalScans == 0) {
      return 0;
    }
    return (double) processedScans / (double) totalScans;

  }

  public String getTaskDescription() {
    return "Gap filling " + peakList;
  }

  PeakList getPeakList() {
    return peakList;
  }

}
