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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.desktop.preferences.MZminePreferences;
import net.sf.mzmine.desktop.preferences.NumOfThreadsParameter;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskStatusListener;

/**
 * The main task creates sub tasks to perform the PeakFinder algorithm on multiple threads. Each sub
 * task performs gap filling on a number of RawDataFiles.
 * 
 * @author Robin Schmid (robinschmid@wwu.de)
 *
 */
class MultiThreadPeakFinderMainTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private ParameterSet parameters;
  private PeakList peakList, processedPeakList;
  private String suffix;
  private boolean removeOriginal;

  private double progress = 0;

  private boolean useLock;

  public MultiThreadPeakFinderMainTask(MZmineProject project, PeakList peakList,
      ParameterSet parameters) {
    this.project = project;
    this.peakList = peakList;
    this.parameters = parameters;

    suffix = parameters.getParameter(MultiThreadPeakFinderParameters.suffix).getValue();
    useLock = parameters.getParameter(MultiThreadPeakFinderParameters.useLock).getValue();
    removeOriginal = parameters.getParameter(MultiThreadPeakFinderParameters.autoRemove).getValue();
  }

  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Running multi threaded gap filler on " + peakList);

    // create lock to synchronize addPeak to processedPeakList in sub tasks
    Lock lock = useLock ? new ReentrantLock() : null;

    // Create new results peak list
    processedPeakList = createResultsPeakList();
    progress = 0.5;

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

    // listener for status change: Cancel / error
    TaskStatusListener list = new TaskStatusListener() {
      @Override
      public void taskStatusChanged(Task task, TaskStatus newStatus, TaskStatus oldStatus) {
        if (newStatus.equals(TaskStatus.CANCELED) || newStatus.equals(TaskStatus.ERROR)) {
          // if one is cancelled, stop all
          synchronized (this) {
            // remove listener
            // cancel all
            for (Task t : tasks) {
              if (t instanceof AbstractTask)
                ((AbstractTask) t).removeTaskStatusListener(this);
              t.cancel();
            }
          }
        }
      }
    };

    // add listener to all sub tasks
    for (Task t : tasks)
      if (t instanceof AbstractTask)
        ((AbstractTask) t).addTaskStatusListener(list);

    // listener will take care of adding the final list
    progress = 1;
    // end
    logger.info("All sub tasks started for multi threaded gap-filling on " + peakList);
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

  /**
   * Distributes the RawDataFiles on different tasks
   * 
   * @param lock
   * @param raw
   * @param maxRunningThreads
   * @param listener
   * @return
   */
  private Task[] createSubTasks(Lock lock, int raw, int maxRunningThreads,
      SubTaskFinishListener listener) {
    int numPerTask = raw / maxRunningThreads;
    int rest = raw % maxRunningThreads;
    Task[] tasks = new Task[maxRunningThreads];
    for (int i = 0; i < maxRunningThreads; i++) {
      int start = numPerTask * i;
      int endexcl = numPerTask * (i + 1);
      // add one from the rest
      if (rest > 0) {
        start += Math.min(i, rest);
        endexcl += Math.min(i + 1, rest);
      }

      if (i == maxRunningThreads - 1)
        endexcl = raw;

      // create task
      tasks[i] = new MultiThreadPeakFinderTask(project, peakList, processedPeakList, parameters,
          start, endexcl, lock, listener);
    }
    return tasks;
  }

  public double getFinishedPercentage() {
    return progress;

  }

  public String getTaskDescription() {
    return "Main task: Gap filling " + peakList;
  }

  PeakList getPeakList() {
    return peakList;
  }

}
