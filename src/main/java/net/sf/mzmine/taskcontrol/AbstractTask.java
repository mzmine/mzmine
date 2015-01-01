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

package net.sf.mzmine.taskcontrol;

/**
 * An abstract implementation of task which defines common methods to make Task
 * implementation easier
 */
public abstract class AbstractTask implements Task {

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage = null;

    /**
     * @see net.sf.mzmine.taskcontrol.Task#setStatus()
     */
    public final void setStatus(TaskStatus newStatus) {
	this.status = newStatus;
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
     */
    public final void setErrorMessage(String errorMessage) {
	this.errorMessage = errorMessage;
    }

    /**
     * Returns the TaskStatus of this Task
     * 
     * @return The current status of this task
     */
    public final TaskStatus getStatus() {
	return this.status;
    }

}
