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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.desktop.preferences.MZminePreferences;
import net.sf.mzmine.desktop.preferences.NumOfThreadsParameter;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfinder.Gap;
import net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfinder.RegressionInfo;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;

class MultiThreadPeakFinderMainTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private ParameterSet parameters;
  private PeakList peakList, processedPeakList;
  private String suffix;
  private double intTolerance;
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  private int finishedTasks, totalTasks;
  private boolean MASTERLIST = true, removeOriginal;
  private int masterSample = 0;

  public MultiThreadPeakFinderMainTask(MZmineProject project, PeakList peakList,
      ParameterSet parameters) {
    this.project = project;
    this.peakList = peakList;
    this.parameters = parameters;

    suffix = parameters.getParameter(MultiThreadPeakFinderParameters.suffix).getValue();
    intTolerance = parameters.getParameter(MultiThreadPeakFinderParameters.intTolerance).getValue();
    mzTolerance = parameters.getParameter(MultiThreadPeakFinderParameters.MZTolerance).getValue();
    rtTolerance = parameters.getParameter(MultiThreadPeakFinderParameters.RTTolerance).getValue();
    removeOriginal = parameters.getParameter(MultiThreadPeakFinderParameters.autoRemove).getValue();
  }

  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Running multi threaded gap filler on " + peakList);

    // create lock to synchronize addPeak to processedPeakList in sub tasks
    Lock lock = new ReentrantLock();

    // Create new results peak list
    processedPeakList = createResultsPeakList();

    // split raw data files into groups for each thread (task)
    // Obtain the settings of max concurrent threads
    // as this task uses one thread
    int maxRunningThreads = getMaxThreads();
    // raw files
    int raw = peakList.getNumberOfRawDataFiles();

    // create consumer of resultpeaklist
    SubTaskFinishListener listener =
        new SubTaskFinishListener(project, parameters, peakList, removeOriginal, maxRunningThreads);

    // Submit the tasks to the task controller for processing
    Task[] tasks = createSubTasks(lock, raw, maxRunningThreads, listener);
    MZmineCore.getTaskController().addTasks(tasks);

    // listener will take care of adding the final list

    // end
    logger.info("Finished multi threaded gap-filling on " + peakList);
    setStatus(TaskStatus.FINISHED);
  }

  private PeakList createResultsPeakList() {
    SimplePeakList processedPeakList =
        new SimplePeakList(peakList + " " + suffix, peakList.getRawDataFiles());

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
    return processedPeakList;
  }

  private int getMaxThreads() {
    int maxRunningThreads = 1;
    NumOfThreadsParameter parameter =
        MZmineCore.getConfiguration().getPreferences().getParameter(MZminePreferences.numOfThreads);
    if (parameter.isAutomatic() || (parameter.getValue() == null))
      maxRunningThreads = Runtime.getRuntime().availableProcessors();
    else
      maxRunningThreads = parameter.getValue();

    // raw files
    int raw = peakList.getNumberOfRawDataFiles();
    // raw files<?
    if (raw < maxRunningThreads)
      maxRunningThreads = raw;
    return maxRunningThreads;
  }

  private Task[] createSubTasks(Lock lock, int raw, int maxRunningThreads,
      SubTaskFinishListener listener) {
    int numPerTask = raw / maxRunningThreads;
    Task[] tasks = new Task[maxRunningThreads];
    for (int i = 0; i < maxRunningThreads; i++) {
      int start = numPerTask * i;
      int endexcl = i < maxRunningThreads - 1 ? numPerTask * (i + 1) : raw;

      // create task
      tasks[i] = new MultiThreadPeakFinderTask(project, peakList, processedPeakList, parameters,
          start, endexcl, lock, listener);
    }
    return tasks;
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
          finishedTasks += datafile1.getNumOfScans();
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
          finishedTasks++;
        }

        // Finalize gaps
        for (Gap gap : gaps) {
          gap.noMoreOffers();
        }
      }
    }
  }

  public double getFinishedPercentage() {
    if (totalTasks == 0) {
      return 0;
    }
    return (double) finishedTasks / (double) totalTasks;

  }

  public String getTaskDescription() {
    return "Sub task: Gap filling " + peakList;
  }

  PeakList getPeakList() {
    return peakList;
  }

}
