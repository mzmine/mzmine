/*
 * Copyright 2006-2020 The MZmine Development Team
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

package io.github.mzmine.modules.batchmode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MZmineProjectListener;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsSelection;
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

    private final List<RawDataFile> createdDataFiles, previousCreatedDataFiles;
    private final List<PeakList> createdPeakLists, previousCreatedPeakLists;

    BatchTask(MZmineProject project, ParameterSet parameters) {
        this.project = project;
        this.queue = parameters.getParameter(BatchModeParameters.batchQueue)
                .getValue();
        totalSteps = queue.size();
        createdDataFiles = new ArrayList<>();
        createdPeakLists = new ArrayList<>();
        previousCreatedDataFiles = new ArrayList<>();
        previousCreatedPeakLists = new ArrayList<>();
    }

    @Override
    public void run() {

        setStatus(TaskStatus.PROCESSING);
        logger.info("Starting a batch of " + totalSteps + " steps");

        // Listen for new items in the project
        MZmineProjectListener listener = new MZmineProjectListener() {
            @Override
            public void peakListAdded(PeakList newPeakList) {
                createdPeakLists.add(newPeakList);
            }

            @Override
            public void dataFileAdded(RawDataFile newFile) {
                createdDataFiles.add(newFile);
            }
        };
        project.addProjectListener(listener);

        // Process individual batch steps
        for (int i = 0; i < totalSteps; i++) {

            processQueueStep(i);
            processedSteps++;

            // Update the project reference in case new project was loaded
            if (project != MZmineCore.getProjectManager().getCurrentProject()) {
                project.removeProjectListener(listener);
                project = MZmineCore.getProjectManager().getCurrentProject();
                project.addProjectListener(listener);
            }

            // If we are canceled or ran into error, stop here
            if (isCanceled() || (getStatus() == TaskStatus.ERROR)) {
                return;
            }

        }

        project.removeProjectListener(listener);

        logger.info("Finished a batch of " + totalSteps + " steps");
        setStatus(TaskStatus.FINISHED);

    }

    private void processQueueStep(int stepNumber) {

        logger.info("Starting step # " + (stepNumber + 1));

        // Run next step of the batch
        MZmineProcessingStep<?> currentStep = queue.get(stepNumber);
        MZmineProcessingModule method = (MZmineProcessingModule) currentStep
                .getModule();
        ParameterSet batchStepParameters = currentStep.getParameterSet();

        // If the last step did not produce any data files or feature lists, use
        // the ones from the previous step
        if (createdDataFiles.isEmpty())
            createdDataFiles.addAll(previousCreatedDataFiles);
        if (createdPeakLists.isEmpty())
            createdPeakLists.addAll(previousCreatedPeakLists);

        // Update the RawDataFilesParameter parameters to reflect the current
        // state of the batch
        for (Parameter<?> p : batchStepParameters.getParameters()) {
            if (p instanceof RawDataFilesParameter) {
                RawDataFilesParameter rdp = (RawDataFilesParameter) p;
                RawDataFile createdFiles[] = createdDataFiles
                        .toArray(new RawDataFile[0]);
                final RawDataFilesSelection selectedFiles = rdp.getValue();
                if (selectedFiles == null) {
                    setStatus(TaskStatus.ERROR);
                    setErrorMessage("Invalid parameter settings for module "
                            + method.getName() + ": "
                            + "Missing parameter value for " + p.getName());
                    return;
                }
                selectedFiles.setBatchLastFiles(createdFiles);
            }
        }

        // Update the PeakListsParameter parameters to reflect the current
        // state of the batch
        for (Parameter<?> p : batchStepParameters.getParameters()) {
            if (p instanceof PeakListsParameter) {
                PeakListsParameter rdp = (PeakListsParameter) p;
                PeakList createdPls[] = createdPeakLists
                        .toArray(new PeakList[0]);
                final PeakListsSelection selectedPeakLists = rdp.getValue();
                if (selectedPeakLists == null) {
                    setStatus(TaskStatus.ERROR);
                    setErrorMessage("Invalid parameter settings for module "
                            + method.getName() + ": "
                            + "Missing parameter value for " + p.getName());
                    return;
                }
                selectedPeakLists.setBatchLastPeakLists(createdPls);
            }
        }

        // Clear the saved data files and feature lists. Save them to the
        // "previous" lists, in case the next step does not produce any new data
        previousCreatedDataFiles.clear();
        previousCreatedDataFiles.addAll(createdDataFiles);
        previousCreatedPeakLists.clear();
        previousCreatedPeakLists.addAll(createdPeakLists);
        createdDataFiles.clear();
        createdPeakLists.clear();

        // Check if the parameter settings are valid
        ArrayList<String> messages = new ArrayList<String>();
        boolean paramsCheck = batchStepParameters
                .checkParameterValues(messages);
        if (!paramsCheck) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage(
                    "Invalid parameter settings for module " + method.getName()
                            + ": " + Arrays.toString(messages.toArray()));
        }

        ArrayList<Task> currentStepTasks = new ArrayList<Task>();
        ExitCode exitCode = method.runModule(project, batchStepParameters,
                currentStepTasks);

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
        MZmineCore.getTaskController()
                .addTasks(currentStepTasks.toArray(new Task[0]));

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
                    setErrorMessage(stepTask.getTaskDescription() + ": "
                            + stepTask.getErrorMessage());
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
