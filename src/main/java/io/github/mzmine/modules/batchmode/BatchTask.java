/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.batchmode;

import io.github.mzmine.datamodel.data.FeatureList;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import com.google.common.collect.ImmutableList;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExitCode;

/**
 * Batch mode task
 */
public class BatchTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private int totalSteps, processedSteps;

  private MZmineProject project;
  private final BatchQueue queue;

  private List<RawDataFile> createdDataFiles, previousCreatedDataFiles;
  private List<FeatureList> createdFeatureLists, previousCreatedFeatureLists;

  BatchTask(MZmineProject project, ParameterSet parameters) {
    this.project = project;
    this.queue = parameters.getParameter(BatchModeParameters.batchQueue).getValue();
    totalSteps = queue.size();
    createdDataFiles = new ArrayList<>();
    createdFeatureLists = new ArrayList<>();
    previousCreatedDataFiles = new ArrayList<>();
    previousCreatedFeatureLists = new ArrayList<>();
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("Starting a batch of " + totalSteps + " steps");



    // Process individual batch steps
    for (int i = 0; i < totalSteps; i++) {

      processQueueStep(i);
      processedSteps++;

      // If we are canceled or ran into error, stop here
      if (isCanceled() || (getStatus() == TaskStatus.ERROR)) {
        return;
      }

    }

    logger.info("Finished a batch of " + totalSteps + " steps");
    setStatus(TaskStatus.FINISHED);

  }

  private void processQueueStep(int stepNumber) {

    logger.info("Starting step # " + (stepNumber + 1));

    // Run next step of the batch
    MZmineProcessingStep<?> currentStep = queue.get(stepNumber);
    MZmineProcessingModule method = (MZmineProcessingModule) currentStep.getModule();
    ParameterSet batchStepParameters = currentStep.getParameterSet();

    final List<FeatureList> beforeFeatureLists = ImmutableList.copyOf(project.getFeatureLists());
    final List<RawDataFile> beforeDataFiles = ImmutableList.copyOf(project.getRawDataFiles());

    // If the last step did not produce any data files or feature lists, use
    // the ones from the previous step
    if (createdDataFiles.isEmpty())
      createdDataFiles = previousCreatedDataFiles;
    if (createdFeatureLists.isEmpty())
      createdFeatureLists = previousCreatedFeatureLists;

    // Update the RawDataFilesParameter parameters to reflect the current
    // state of the batch
    for (Parameter<?> p : batchStepParameters.getParameters()) {
      if (p instanceof RawDataFilesParameter) {
        RawDataFilesParameter rdp = (RawDataFilesParameter) p;
        RawDataFile createdFiles[] = createdDataFiles.toArray(new RawDataFile[0]);
        final RawDataFilesSelection selectedFiles = rdp.getValue();
        if (selectedFiles == null) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("Invalid parameter settings for module " + method.getName() + ": "
              + "Missing parameter value for " + p.getName());
          return;
        }
        selectedFiles.setBatchLastFiles(createdFiles);
      }
    }

    createdDataFiles = new ArrayList<>(project.getRawDataFiles());
    createdFeatureLists = new ArrayList<>(project.getFeatureLists());
    createdDataFiles.removeAll(beforeDataFiles);
    createdFeatureLists.removeAll(beforeFeatureLists);

    // Update the FeatureListsParameter parameters to reflect the current
    // state of the batch
    for (Parameter<?> p : batchStepParameters.getParameters()) {
      if (p instanceof FeatureListsParameter) {
        FeatureListsParameter rdp = (FeatureListsParameter) p;
        FeatureList createdPls[] = createdFeatureLists.toArray(new FeatureList[0]);
        final FeatureListsSelection selectedFeatureLists = rdp.getValue();
        if (selectedFeatureLists == null) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("Invalid parameter settings for module " + method.getName() + ": "
              + "Missing parameter value for " + p.getName());
          return;
        }
        selectedFeatureLists.setBatchLastPeakLists(createdPls);
      }
    }

    // Clear the saved data files and feature lists. Save them to the
    // "previous" lists, in case the next step does not produce any new data
    if (!createdDataFiles.isEmpty())
      previousCreatedDataFiles = createdDataFiles;
    if (!createdFeatureLists.isEmpty())
      previousCreatedFeatureLists = createdFeatureLists;

    // Check if the parameter settings are valid
    ArrayList<String> messages = new ArrayList<String>();
    boolean paramsCheck = batchStepParameters.checkParameterValues(messages);
    if (!paramsCheck) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Invalid parameter settings for module " + method.getName() + ": "
          + Arrays.toString(messages.toArray()));
    }

    ArrayList<Task> currentStepTasks = new ArrayList<Task>();
    ExitCode exitCode = method.runModule(project, batchStepParameters, currentStepTasks);

    if (exitCode != ExitCode.OK) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not start batch step " + method.getName());
      return;
    }

    // If current step didn't produce any tasks, continue with next step
    if (currentStepTasks.isEmpty())
      return;

    boolean allTasksFinished = false;

    // Submit the tasks to the task controller for processing
    MZmineCore.getTaskController().addTasks(currentStepTasks.toArray(new Task[0]));

    while (!allTasksFinished) {

      // If we canceled the batch, cancel all running tasks
      if (isCanceled()) {
        for (Task stepTask : currentStepTasks)
          stepTask.cancel();
        return;
      }

      // First set to true, then check all tasks
      allTasksFinished = true;

      for (Task stepTask : currentStepTasks) {

        TaskStatus stepStatus = stepTask.getStatus();

        // If any of them is not finished, keep checking
        if (stepStatus != TaskStatus.FINISHED)
          allTasksFinished = false;

        // If there was an error, we have to stop the whole batch
        if (stepStatus == TaskStatus.ERROR) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage(stepTask.getTaskDescription() + ": " + stepTask.getErrorMessage());
          return;
        }

        // If user canceled any of the tasks, we have to cancel the
        // whole batch
        if (stepStatus == TaskStatus.CANCELED) {
          setStatus(TaskStatus.CANCELED);
          for (Task t : currentStepTasks)
            t.cancel();
          return;
        }

      }

      // Wait 1s before checking the tasks again
      if (!allTasksFinished) {
        synchronized (this) {
          try {
            this.wait(1000);
          } catch (InterruptedException e) {
            // ignore
          }
        }
      }

    }

  }

  @Override
  public TaskPriority getTaskPriority() {
    // to not block mzmine when run with single thread
    return TaskPriority.HIGH;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalSteps == 0)
      return 0;
    return (double) processedSteps / totalSteps;
  }

  @Override
  public String getTaskDescription() {
    return "Batch of " + totalSteps + " steps";
  }

}
