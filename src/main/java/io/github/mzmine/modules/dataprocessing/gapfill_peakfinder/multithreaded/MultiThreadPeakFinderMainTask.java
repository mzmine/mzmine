/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.AllTasksFinishedListener;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
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

  private static final Logger logger = Logger.getLogger(
      MultiThreadPeakFinderMainTask.class.getName());
  private final MZmineProject project;
  private final OriginalFeatureListOption originalFeatureListOption;
  private final ParameterSet parameters;
  private final ModularFeatureList peakList;
  private final String suffix;
  private final AtomicDouble progress = new AtomicDouble(0);
  private ModularFeatureList processedPeakList;

  /**
   * @param batchTasks all sub tasks are registered to the batchtasks list
   */
  public MultiThreadPeakFinderMainTask(MZmineProject project, FeatureList peakList,
      ParameterSet parameters, Collection<Task> batchTasks, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.project = project;
    this.peakList = (ModularFeatureList) peakList;
    this.parameters = parameters;

    suffix = parameters.getParameter(MultiThreadPeakFinderParameters.suffix).getValue();
    originalFeatureListOption = parameters.getValue(MultiThreadPeakFinderParameters.handleOriginal);
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info(
        () -> String.format("Running multithreaded gap filler on %s (handle original list:%s)",
            peakList.toString(), originalFeatureListOption.toString()));

    // Create new results feature list
    processedPeakList = switch (originalFeatureListOption) {
      case PROCESS_IN_PLACE -> peakList;
      case KEEP, REMOVE -> peakList.createCopy(peakList + " " + suffix, getMemoryMapStorage(),
          false);
    };

    progress.getAndSet(0.1);

    // split raw data files into groups for each thread (task)
    // Obtain the settings of max concurrent threads
    // as this task uses one thread
    int maxRunningThreads = getMaxThreads();
    // raw files
    int raw = processedPeakList.getNumberOfRawDataFiles();

    // Submit the tasks to the task controller for processing
    List<AbstractTask> tasks = createSubTasks(raw, maxRunningThreads);

    final AbstractTask thistask = this;
    new AllTasksFinishedListener(tasks, true,
        // succeed
        l -> {
          logger.info(
              "All sub tasks of multithreaded gap-filling have finished. Finalising results.");

          // Add task description to peakList
          processedPeakList.addDescriptionOfAppliedTask(
              new SimpleFeatureListAppliedMethod("Gap filling ", MultiThreadPeakFinderModule.class,
                  parameters, getModuleCallDate()));

          // update all rows by row bindings (average values)
          // this needs to be done after all tasks finish because values were not updated when
          // adding features
          processedPeakList.applyRowBindings();

          // add / remove or rename the new feature list in project
          originalFeatureListOption.reflectNewFeatureListToProject(suffix, project,
              processedPeakList, peakList);

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

    // wait till finish
    while (!(isCanceled() || isFinished())) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        logger.log(Level.SEVERE, "Error in GNPS export/submit task", e);
      }
    }
  }

  private int getMaxThreads() {
    int maxRunningThreads = 1;
    NumOfThreadsParameter parameter = MZmineCore.getConfiguration().getPreferences()
        .getParameter(MZminePreferences.numOfThreads);
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
          new MultiThreadPeakFinderTask(peakList, processedPeakList, parameters, start, endexcl, i,
              getModuleCallDate()));
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

}
