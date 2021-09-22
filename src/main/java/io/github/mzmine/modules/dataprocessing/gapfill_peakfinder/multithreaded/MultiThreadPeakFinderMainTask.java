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

package io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.gui.preferences.NumOfThreadsParameter;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.AllTasksFinishedListener;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The main task creates sub tasks to perform the PeakFinder algorithm on multiple threads. Each sub
 * task performs gap filling on a number of RawDataFiles.
 *
 * @author Robin Schmid (robinschmid@wwu.de)
 */
class MultiThreadPeakFinderMainTask extends AbstractTask {

  private final MZmineProject project;
  private Logger logger = Logger.getLogger(this.getClass().getName());
  private ParameterSet parameters;
  private ModularFeatureList peakList, processedPeakList;
  private String suffix;
  private boolean removeOriginal;

  private AtomicDouble progress = new AtomicDouble(0);
  private Collection<Task> batchTasks;

  /**
   * @param batchTasks all sub tasks are registered to the batchtasks list
   */
  public MultiThreadPeakFinderMainTask(MZmineProject project, FeatureList peakList,
      ParameterSet parameters, Collection<Task> batchTasks, @Nullable MemoryMapStorage storage, @NotNull Date moduleCallDate) {
    super(storage, moduleCallDate);
    this.project = project;
    this.peakList = (ModularFeatureList) peakList;
    this.parameters = parameters;
    this.batchTasks = batchTasks;

    suffix = parameters.getParameter(MultiThreadPeakFinderParameters.suffix).getValue();
    removeOriginal = parameters.getParameter(MultiThreadPeakFinderParameters.autoRemove).getValue();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Running multithreaded gap filler on " + peakList);

    // Create new results feature list
    processedPeakList = peakList.createCopy(peakList + " " + suffix, getMemoryMapStorage(), false);
    progress.getAndSet(0.1);

    // split raw data files into groups for each thread (task)
    // Obtain the settings of max concurrent threads
    // as this task uses one thread
    int maxRunningThreads = getMaxThreads();
    // raw files
    int raw = processedPeakList.getNumberOfRawDataFiles();

    // create consumer of resultpeaklist
//    SubTaskFinishListener listener =
//        new SubTaskFinishListener(project, parameters, peakList, removeOriginal, maxRunningThreads);

    // Submit the tasks to the task controller for processing
    List<AbstractTask> tasks = createSubTasks(raw, maxRunningThreads);

    // add listener to all sub tasks
//    for (Task t : tasks) {
//      // add to batchMode collection
//      if (batchTasks != null) {
//        batchTasks.add(t);
//      }
//    }
    final AbstractTask thistask = this;
    new AllTasksFinishedListener(tasks, true,
        // succeed
        l -> {
          logger.info(
              "All sub tasks of multithreaded gap-filling have finished. Finalising results.");
          // add pkl to project
          // Append processed feature list to the project
          project.addFeatureList(processedPeakList);

          // Add quality parameters to peaks
          //QualityParameters.calculateQualityParameters(processedPeakList);

          // Add task description to peakList
          processedPeakList
              .addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod("Gap filling ",
                  MultiThreadPeakFinderModule.class, parameters));

          // Remove the original peaklist if requested
          if (removeOriginal) {
            project.removeFeatureList(peakList);
          }

          logger.info("Completed: Multithreaded gap-filling successfull");

          if (thistask.getStatus() == TaskStatus.PROCESSING) {
            thistask.setStatus(TaskStatus.FINISHED);
          }
        }, lerror -> {
      setErrorMessage("Error in gap filling");
      thistask.setStatus(TaskStatus.ERROR);
      for (AbstractTask task : tasks) {
        task.setStatus(TaskStatus.ERROR);
      }
    },
        // cancel if one was cancelled
        listCancelled -> cancel()) {
      @Override
      public void taskStatusChanged(Task task, TaskStatus newStatus, TaskStatus oldStatus) {
        super.taskStatusChanged(task, newStatus, oldStatus);
        // show progress
        if (oldStatus != newStatus && newStatus == TaskStatus.FINISHED) {
          progress.getAndAdd(0.9 / tasks.size());
        }
      }
    };

    // start
    MZmineCore.getTaskController().addTasks(tasks.toArray(AbstractTask[]::new));

//    // wait till finish
    while (!(isCanceled() || isFinished())) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        logger.log(Level.SEVERE, "Error in GNPS export/submit task", e);
      }
    }
//
//    // listener will take care of adding the final list
//    progress.getAndSet(1d);
//    // end
//    logger.info("All sub tasks started for multithreaded gap-filling on " + peakList);
//    setStatus(TaskStatus.FINISHED);
  }

  private int getMaxThreads() {
    int maxRunningThreads = 1;
    NumOfThreadsParameter parameter =
        MZmineCore.getConfiguration().getPreferences().getParameter(MZminePreferences.numOfThreads);
    if (parameter.isAutomatic() || (parameter.getValue() == null)) {
      maxRunningThreads = Runtime.getRuntime().availableProcessors();
    } else {
      maxRunningThreads = parameter.getValue();
    }

    // raw files
    int raw = peakList.getNumberOfRawDataFiles();
    // raw files<?
    if (raw < maxRunningThreads) {
      maxRunningThreads = raw;
    }
    return maxRunningThreads;
  }

  /**
   * Distributes the RawDataFiles on different tasks
   */
  private List<AbstractTask> createSubTasks(int raw, int maxRunningThreads) {
    int numPerTask = raw / maxRunningThreads;
    int rest = raw % maxRunningThreads;
    List<AbstractTask> tasks = new ArrayList<>();
    for (int i = 0; i < maxRunningThreads; i++) {
      int start = numPerTask * i;
      int endexcl = numPerTask * (i + 1);
      // add one from the rest
      if (rest > 0) {
        start += Math.min(i, rest);
        endexcl += Math.min(i + 1, rest);
      }

      if (i == maxRunningThreads - 1) {
        endexcl = raw;
      }

      // create task
      tasks.add(
          new MultiThreadPeakFinderTask(peakList, processedPeakList, parameters, start, endexcl,
              i, getModuleCallDate()));
    }
    return tasks;
  }

  @Override
  public double getFinishedPercentage() {
    return progress.get();
  }

  @Override
  public String getTaskDescription() {
    return "Main task: Gap filling " + peakList;
  }

  FeatureList getPeakList() {
    return peakList;
  }

}
