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

package net.sf.mzmine.taskcontrol.impl;

import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.Task;

/**
 * This class serves as a replacement for Task within the task controller queue,
 * after the Task is finished. This allows the garbage collector to remove the
 * memory occupied by the actual Task while keeping the task description in the
 * Tasks in progress window, until all tasks are finished.
 */
public class FinishedTask extends AbstractTask {

    private String description;
    private double finishedPercentage;

    public FinishedTask(Task task) {
	setStatus(task.getStatus());
	setErrorMessage(task.getErrorMessage());
	description = task.getTaskDescription();
	finishedPercentage = task.getFinishedPercentage();
    }

    public String getTaskDescription() {
	return description;
    }

    public void run() {
	// ignore any attempt to run this task, because it is finished
    }

    public void cancel() {
	// ignore any attempt to cancel this task, because it is finished
    }

    public double getFinishedPercentage() {
	return finishedPercentage;
    }

}
