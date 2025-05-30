/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.gui.preferences.NumOfThreadsParameter;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.javafx.components.factories.FxTexts;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.AllTasksFinishedListener;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.utils.TaskResultSummary.ErrorMessageHandling;
import io.github.mzmine.taskcontrol.utils.TaskUtils;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.mzio.links.MzioMZmineLinks;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    checkTotalWorkloadAndMemory();

    final int numRows = peakList.getNumberOfRows();
    // raw files
    int raw = peakList.getNumberOfRawDataFiles();

    // Create new results feature list
    processedPeakList = switch (originalFeatureListOption) {
      case PROCESS_IN_PLACE -> peakList;
      // already estimate the number of features to avoid resizing
      case KEEP, REMOVE ->
          FeatureListUtils.createCopy(peakList, null, suffix, getMemoryMapStorage(), true,
              peakList.getRawDataFiles(), false, numRows,
              FeatureListUtils.estimateFeatures(numRows, raw));
    };

    progress.getAndSet(0.1);

    // split raw data files into groups for each thread (task)
    // Obtain the settings of max concurrent threads
    // as this task uses one thread
    int maxRunningThreads = getMaxThreads();

    // total gaps
    long totalFeatures = numRows * (long) raw;
    long detectedFeatures = peakList.stream().mapToLong(FeatureListRow::getNumberOfFeatures).sum();
    long totalGaps = totalFeatures - detectedFeatures;
    System.gc();
    double usedGbBefore = ConfigService.getConfiguration().getUsedMemoryGB();

    // Submit the tasks to the task controller for processing
    List<AbstractTask> tasks = createSubTasks(raw, maxRunningThreads);

    final AbstractTask thistask = this;
    new AllTasksFinishedListener(tasks, true,
        // succeed
        l -> {
          // do nothing on success here - needs to happen on main task thread
        },
        // on error
        lerror -> {
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
    var wrappedTasks = MZmineCore.getTaskController().addTasks(tasks.toArray(AbstractTask[]::new));
    TaskUtils.waitForTasksToFinish(thistask, wrappedTasks);

    // make sure tasks all finished successfully
    var worstResult = TaskUtils.findWorstResult(ErrorMessageHandling.COMBINE_UNIQUE, wrappedTasks);
    // error or cancel may be applied here if the listener was to slow
    worstResult.applyToTask(thistask);

    if (isCanceled()) {
      return;
    }

    logger.info("All sub tasks of multithreaded gap-filling have finished. Finalising results.");

    // Add task description to peakList
    processedPeakList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Gap filling ", MultiThreadPeakFinderModule.class,
            parameters, getModuleCallDate()));

    // update all rows by row bindings (average values)
    // this needs to be done after all tasks finish because values were not updated when
    // adding features
    processedPeakList.applyRowBindings();

    // add / remove or rename the new feature list in project
    originalFeatureListOption.reflectNewFeatureListToProject(suffix, project, processedPeakList,
        peakList);

    logger.info("Completed: Multithreaded gap-filling successfull");

    if (thistask.getStatus() == TaskStatus.PROCESSING) {
      thistask.setStatus(TaskStatus.FINISHED);
    }

    long afterGapFill = peakList.stream().mapToLong(FeatureListRow::getNumberOfFeatures).sum();
    System.gc();
    double usedGbAfter = ConfigService.getConfiguration().getUsedMemoryGB();
    logger.info("""
        Gap-filling statistics:
        %d x %d (rows x samples) = %d total possible features
        Initial RAM %.1f GB: %d detected with %d gaps to search
        After RAM %.1f GB: %d total features""".formatted(numRows,
        peakList.getNumberOfRawDataFiles(), totalFeatures, //
        usedGbBefore, detectedFeatures, totalGaps,//
        usedGbAfter, afterGapFill));
  }


  /**
   * Check the estimated memory requirements for this run
   */
  private void checkTotalWorkloadAndMemory() {
    // 5405 x 250 (rows x samples) = 1351250 total possible features
    // Initial RAM 2.0 GB: 288440 detected with 1062810 gaps to search
    // After RAM 2.1 GB: 939281 total features
    //
    // step	secondsToFinish	name	usedHeapGB
    //6	2.656	"Feature list rows filter"	2.00
    //7	6.33	"Feature finder (multithreaded)"	2.13
    // considering difference between rows filter and feature finder memory:
    // 0.13 GB for 650,841 new Features = 0.199741565/ M features
    final int totalRows = peakList.getNumberOfRows();
    final int numRaws = peakList.getNumberOfRawDataFiles();
    final double imsRamFactor = FeatureListUtils.getImsRamFactor(peakList);
    final double optionFactor =
        originalFeatureListOption == OriginalFeatureListOption.PROCESS_IN_PLACE ? 1d : 2d;
    final double gbMemoryPerMillionFeatures = 0.5 * imsRamFactor * optionFactor;
    final double maxMemoryGB = ConfigService.getConfiguration().getMaxMemoryGB();
    final long totalFeatures = totalRows * (long) numRaws;
    final double expectedMemoryUsage = gbMemoryPerMillionFeatures / 1_000_000 * totalFeatures;

    logger.info("""
        Gap-filling started on a total of %d feature list rows across %d samples (%d potential features). \
        Max memory available: %.1f GB. Expecting to use %.1f GB just for gap filling""".formatted(
        totalRows, numRaws, totalFeatures, maxMemoryGB, expectedMemoryUsage));

    // check if memory constrains may arise
    if (expectedMemoryUsage > maxMemoryGB * 0.85) {
      DialogLoggerUtil.showMessageDialog("Large dataset gap-filling", false,
          FxTextFlows.newTextFlow(FxTexts.text("""
                  mzmine gap-filling started on a total of %d feature list rows across %d samples. \
                  This results in a total of %d possible features (rows x samples), that may cause memory constraints.
                  When possible run modules with PROCESS_IN_PLACE where available or with REMOVE option to clear previous results. \
                  Consider applying the feature list rows filter to remove features below X%% detections.
                  Other great filters to reduce the number of noisy features are also found in the chromatogram builder and feature resolvers, \
                  such as increased minimum height, chromatographic threshold, and feature top/edge ratio in the local minimum resolver.
                  When working on large datasets, consult the performance documentation for tuning options:
                  """.formatted(totalRows, numRaws, totalFeatures)),
              FxTexts.hyperlinkText(MzioMZmineLinks.PERFORMANCE_DOCU.getUrl())));
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

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.HIGH;
  }
}
