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

import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskPriority;

/**
 * Wrapper class for Tasks that stores additional information
 */
public class WrappedTask {

    private Task task;
    private TaskPriority priority;
    private WorkerThread assignedTo;

    WrappedTask(Task task, TaskPriority priority) {
	this.task = task;
	this.priority = priority;
    }

    /**
     * @return Returns the priority.
     */
    TaskPriority getPriority() {
	return priority;
    }

    /**
     * @param priority
     *            The priority to set.
     */
    void setPriority(TaskPriority priority) {
	this.priority = priority;
	if (assignedTo != null) {
	    switch (priority) {
	    case HIGH:
		assignedTo.setPriority(Thread.MAX_PRIORITY);
		break;
	    case NORMAL:
		assignedTo.setPriority(Thread.NORM_PRIORITY);
		break;
	    }
	}
    }

    /**
     * @return Returns the assigned.
     */
    boolean isAssigned() {
	return assignedTo != null;
    }

    void assignTo(WorkerThread thread) {
	assignedTo = thread;
    }

    /**
     * @return Returns the task.
     */
    public synchronized Task getActualTask() {
	return task;
    }

    public synchronized String toString() {
	return task.getTaskDescription();
    }

    synchronized void removeTaskReference() {
	task = new FinishedTask(task);
    }

}
