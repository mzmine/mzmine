/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

import java.util.logging.Logger;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * Batch mode task
 */
public class BatchTask implements Task { 

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private TaskStatus taskStatus = TaskStatus.WAITING;
	private String errorMessage;
	private int totalSteps, processedSteps;

	BatchTask(BatchQueue queue, RawDataFile dataFiles[], PeakList peakLists[]) {

	}
	
	public void run() {

        logger.info("Starting a batch of " + totalSteps + " steps");

        /*
		 * BatchStepWrapper newStep = currentBatchSteps.get(currentStep);
		 * BatchStep method = newStep.getMethod();
		 * 
		 * Task[] newSequence = method.runModule(selectedDataFiles,
		 * selectedPeakLists, newStep.getParameters(), this);
		 * 
		 * if (newSequence == null) {
		 * desktop.displayErrorMessage("Batch processing cannot continue.");
		 * batchRunning = false; }
		 */

        logger.info("Starting a batch of " + totalSteps + " steps");

    }

	public void cancel() {
		taskStatus = TaskStatus.CANCELED;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public double getFinishedPercentage() {
		if (totalSteps == 0)
			return 0;
		return (double) processedSteps / totalSteps;
	}

	public TaskStatus getStatus() {
		return taskStatus;
	}

	public String getTaskDescription() {
		return "Batch of " + totalSteps + " steps";
	}

}