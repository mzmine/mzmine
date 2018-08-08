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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfinder.Gap;
import net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfinder.RegressionInfo;
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
  private int processedScans, totalScans;
  private boolean MASTERLIST = true;
  private int masterSample = 0;

  // start and end (exclusive) for raw data file processing
  private int start;
  private int endexcl;

  private Lock lock;

  // takes care of adding the final result
  private SubTaskFinishListener listener;

  MultiThreadPeakFinderTask(MZmineProject project, PeakList peakList, PeakList processedPeakList,
      ParameterSet parameters, int start, int endexcl, Lock lock, SubTaskFinishListener listener) {

    // central lock that blocks only when peaks are added to the processedPeakList
    this.lock = lock;
    this.listener = listener;

    this.project = project;
    this.peakList = peakList;
    this.processedPeakList = processedPeakList;

    suffix = parameters.getParameter(MultiThreadPeakFinderParameters.suffix).getValue();
    intTolerance = parameters.getParameter(MultiThreadPeakFinderParameters.intTolerance).getValue();
    mzTolerance = parameters.getParameter(MultiThreadPeakFinderParameters.MZTolerance).getValue();
    rtTolerance = parameters.getParameter(MultiThreadPeakFinderParameters.RTTolerance).getValue();
    rtCorrection = parameters.getParameter(MultiThreadPeakFinderParameters.RTCorrection).getValue();

    this.start = start;
    this.endexcl = endexcl;
  }

  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("Running multi threaded gap filler on raw files " + start + "-" + (endexcl - 1)
        + " of pkl:" + peakList);

    // Calculate total number of scans in all files
    for (int i = start; i < endexcl; i++) {
      RawDataFile dataFile = peakList.getRawDataFile(i);
      totalScans += dataFile.getNumOfScans(1);
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
      for (int i = start; i < endexcl; i++) {
        RawDataFile dataFile = peakList.getRawDataFile(i);

        // Canceled?
        if (isCanceled()) {
          return;
        }

        List<Gap> gaps = new ArrayList<Gap>();

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
        if (gaps.isEmpty()) {
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
          gap.noMoreOffers(lock);
        }

      }
    }

    // first notify listener
    listener.accept(processedPeakList);

    logger.info("Finished sub task: Multi threaded gap filler on raw files " + start + "-"
        + (endexcl - 1) + " of pkl:" + peakList);
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
    return "Sub task: Gap filling on raw files " + start + "-" + (endexcl - 1) + " of pkl:"
        + peakList;
  }

  PeakList getPeakList() {
    return peakList;
  }

}
