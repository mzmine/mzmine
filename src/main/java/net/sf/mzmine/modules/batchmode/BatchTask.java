/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.batchmode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExitCode;

/**
 * Batch mode task
 */
public class BatchTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private int totalSteps, processedSteps;

    private MZmineProject project;
    private BatchQueue queue;

    BatchTask(MZmineProject project, ParameterSet parameters) {
	this.project = project;
	this.queue = parameters.getParameter(BatchModeParameters.batchQueue)
		.getValue();
	totalSteps = queue.size();
    }

    public void run() {

	setStatus(TaskStatus.PROCESSING);
	logger.info("Starting a batch of " + totalSteps + " steps");

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

	// Run next step of the batch
	MZmineProcessingStep<?> currentStep = queue.get(stepNumber);
	MZmineProcessingModule method = (MZmineProcessingModule) currentStep
		.getModule();
	ParameterSet batchStepParameters = currentStep.getParameterSet();

	// Check if the parameter settings are valid
	ArrayList<String> messages = new ArrayList<String>();
	boolean paramsCheck = batchStepParameters
		.checkParameterValues(messages);
	if (!paramsCheck) {
	    setStatus(TaskStatus.ERROR);
	    setErrorMessage("Invalid parameter settings for module " + method
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
	MZmineCore.getTaskController().addTasks(
		currentStepTasks.toArray(new Task[0]));

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
		    setErrorMessage(stepTask.getErrorMessage());
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

    public double getFinishedPercentage() {
	if (totalSteps == 0)
	    return 0;
	return (double) processedSteps / totalSteps;
    }

    public String getTaskDescription() {
	return "Batch of " + totalSteps + " steps";
    }

}
