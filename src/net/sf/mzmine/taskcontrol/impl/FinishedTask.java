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

package net.sf.mzmine.taskcontrol.impl;

import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * This class serves as a replacement for Task within the task controller queue,
 * after the Task is finished. This allows the garbage collector to remove the
 * memory occupied by the actual Task while keeping the task description in the
 * Tasks in progress window, until all tasks are finished.
 */
public class FinishedTask implements Task {

	private String description, errorMessage;
	private TaskStatus status;
	private double finishedPercentage;

	public FinishedTask(Task task) {
		description = task.getTaskDescription();
		errorMessage = task.getErrorMessage();
		status = task.getStatus();
		finishedPercentage = task.getFinishedPercentage();
	}

	public void cancel() {
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public double getFinishedPercentage() {
		return finishedPercentage;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public String getTaskDescription() {
		return description;
	}

	public void run() {
		throw new IllegalStateException();
	}

	public Object[] getCreatedObjects() {
		return null;
	}

}
