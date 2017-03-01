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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.taskcontrol.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;

/**
 * Task controller worker thread, this thread will process one task and then
 * finish
 */
class WorkerThread extends Thread {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private WrappedTask wrappedTask;
    private boolean finished = false;

    WorkerThread(WrappedTask wrappedTask) {
	super("Thread executing task " + wrappedTask);
	this.wrappedTask = wrappedTask;
	wrappedTask.assignTo(this);
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

	Task actualTask = wrappedTask.getActualTask();

	try {

	    // Log the start (INFO level events go to the Status bar, too)
	    logger.info("Starting processing of task "
		    + actualTask.getTaskDescription());

	    // Process the actual task
	    actualTask.run();

	    // Check if task finished with an error
	    if (actualTask.getStatus() == TaskStatus.ERROR) {

		String errorMsg = actualTask.getErrorMessage();
		if (errorMsg == null)
		    errorMsg = "Unspecified error";

		// Log the error
		logger.severe("Error of task "
			+ actualTask.getTaskDescription() + ": " + errorMsg);

		MZmineCore.getDesktop().displayErrorMessage(
			MZmineCore.getDesktop().getMainWindow(),
			"Error of task " + actualTask.getTaskDescription(),
			errorMsg);
	    } else {
		// Log the finish
		logger.info("Processing of task "
			+ actualTask.getTaskDescription() + " done, status "
			+ actualTask.getStatus());
	    }

	    /*
	     * This is important to allow the garbage collector to remove the
	     * task, while keeping the task description in the
	     * "Tasks in progress" window
	     */
	    wrappedTask.removeTaskReference();

	} catch (Throwable e) {

	    /*
	     * This should never happen, it means the task did not handle its
	     * exception properly, or there was some severe error, like
	     * OutOfMemoryError
	     */

	    logger.log(Level.SEVERE,
		    "Unhandled exception " + e + " while processing task "
			    + actualTask.getTaskDescription(), e);

	    e.printStackTrace();

	    MZmineCore.getDesktop().displayErrorMessage(
		    MZmineCore.getDesktop().getMainWindow(),
		    "Unhandled exception in task "
			    + actualTask.getTaskDescription() + ": "
			    + ExceptionUtils.exceptionToString(e));

	}

	/*
	 * Mark this thread as finished
	 */
	finished = true;

    }

    boolean isFinished() {
	return finished;
    }

}
