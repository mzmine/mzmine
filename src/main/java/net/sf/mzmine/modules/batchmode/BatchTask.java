/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.RawDataFilesParameter;
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

    private BatchQueue queue;

    private RawDataFile dataFiles[];
    private PeakList peakLists[];

    BatchTask(ParameterSet parameters) {
	this.queue = parameters.getParameter(BatchModeParameters.batchQueue)
		.getValue();
	this.dataFiles = parameters.getParameter(BatchModeParameters.dataFiles)
		.getValue();
	this.peakLists = parameters.getParameter(BatchModeParameters.peakLists)
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
	MZmineProcessingStep currentStep = queue.get(stepNumber);
	MZmineProcessingModule method = (MZmineProcessingModule) currentStep
		.getModule();
	ParameterSet batchStepParameters = currentStep.getParameterSet();

	// Update dataFiles and peakLists in the batchStepParameters
	for (Parameter p : batchStepParameters.getParameters()) {
	    if (p instanceof RawDataFilesParameter) {
		RawDataFilesParameter rdp = (RawDataFilesParameter) p;
		rdp.setValue(dataFiles);
	    }
	    if (p instanceof PeakListsParameter) {
		PeakListsParameter plp = (PeakListsParameter) p;
		plp.setValue(peakLists);
	    }
	}

	// Check if the parameter settings are valid
	ArrayList<String> messages = new ArrayList<String>();
	boolean paramsCheck = batchStepParameters
		.checkAllParameterValues(messages);
	if (!paramsCheck) {
	    setStatus(TaskStatus.ERROR);
	    errorMessage = "Invalid parameter settings for module " + method
		    + ": " + Arrays.toString(messages.toArray());
	}

	ArrayList<Task> currentStepTasks = new ArrayList<Task>();
	ExitCode exitCode = method.runModule(batchStepParameters,
		currentStepTasks);

	if (exitCode != ExitCode.OK) {
	    setStatus(TaskStatus.ERROR);
	    errorMessage = "Could not start batch step " + method.getName();
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
		    errorMessage = stepTask.getErrorMessage();
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

	// Now all tasks are finished. We have to check if project was modified.
	// If any raw data files or peak lists were added, we continue batch
	// processing on those.

	Vector<RawDataFile> newDataFiles = new Vector<RawDataFile>();
	Vector<PeakList> newPeakLists = new Vector<PeakList>();

	for (Task stepTask : currentStepTasks) {
	    Object createdObjects[] = stepTask.getCreatedObjects();
	    if (createdObjects == null)
		continue;
	    for (Object createdObject : createdObjects) {
		if (createdObject instanceof RawDataFile)
		    newDataFiles.add((RawDataFile) createdObject);
		if (createdObject instanceof PeakList)
		    newPeakLists.add((PeakList) createdObject);
	    }
	}

	if (newDataFiles.size() > 0)
	    dataFiles = newDataFiles.toArray(new RawDataFile[0]);
	if (newPeakLists.size() > 0)
	    peakLists = newPeakLists.toArray(new PeakList[0]);

    }

    public double getFinishedPercentage() {
	if (totalSteps == 0)
	    return 0;
	return (double) processedSteps / totalSteps;
    }

    public String getTaskDescription() {
	return "Batch of " + totalSteps + " steps";
    }

    public Object[] getCreatedObjects() {
	return null;
    }

}
