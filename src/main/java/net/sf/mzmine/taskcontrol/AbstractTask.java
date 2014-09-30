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

package net.sf.mzmine.taskcontrol;

import java.util.LinkedList;

/**
 * An abstract implementation of task which defines common methods to make Task
 * implementation easier
 */
public abstract class AbstractTask implements Task {

	private TaskStatus status = TaskStatus.WAITING;
	private LinkedList<TaskListener> taskListeners = new LinkedList<TaskListener>();
	protected String errorMessage = null;

	/**
	 * Adds a TaskListener to this Task
	 * 
	 * @param t
	 *            The TaskListener to add
	 */
	public void addTaskListener(TaskListener t) {
		taskListeners.add(t);
	}

	/**
	 * Returns all of the TaskListeners which are listening to this task.
	 * 
	 * @return An array containing the TaskListeners
	 */
	public TaskListener[] getTaskListeners() {
		return taskListeners.toArray(new TaskListener[0]);
	}

	/**
	 * Triggers a TaskEvent and notifies the listeners
	 */
	private void fireTaskEvent() {
		TaskEvent event = new TaskEvent(this);
		for (TaskListener t : this.taskListeners) {
			t.statusChanged(event);
		}
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#setStatus()
	 */
	public final void setStatus(TaskStatus newStatus) {
		this.status = newStatus;
		this.fireTaskEvent();
	}

	/**
	 * Convenience method for determining if this task has been canceled. Also
	 * returns true if the task encountered an error.
	 * 
	 * @return true if this task has been canceled or stopped due to an error
	 */
	public final boolean isCanceled() {
		return (status == TaskStatus.CANCELED) || (status == TaskStatus.ERROR);
	}

	/**
	 * Convenience method for determining if this task has been completed
	 * 
	 * @return true if this task is finished
	 */
	public final boolean isFinished() {
		return status == TaskStatus.FINISHED;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
	public void cancel() {
		setStatus(TaskStatus.CANCELED);
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
	 */
	public final String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * Returns the TaskStatus of this Task
	 * 
	 * @return The current status of this task
	 */
	public final TaskStatus getStatus() {
		return this.status;
	}

	public Object[] getCreatedObjects() {
		return null;
	}

}
