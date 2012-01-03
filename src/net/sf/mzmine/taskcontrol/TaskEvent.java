/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

import java.util.EventObject;

/**
 * A class for relaying Changes in TaskStatus to listeners
 */
public class TaskEvent extends EventObject {
	
	private TaskStatus status;

	/**
	 * Creates a new TaskEvent
	 * 
	 * @param source
	 *            The Task which caused this event.
	 */
	public TaskEvent(Task source) {
		super(source);
		this.status = source.getStatus();
	}

	/**
	 * Creates a new TaskEvent
	 * 
	 * @param source
	 *            The Task which caused this event.
	 * @param status
	 *            The new TaskStatus of the Task.
	 */
	public TaskEvent(Task source, TaskStatus status) {
		super(source);
		this.status = status;
	}

	/**
	 * Get the source of this TaskEvent
	 * 
	 * @return The Task which caused this event
	 */
	public Task getSource() {
		return (Task) this.source;
	}

	/**
	 * Get the new TaskStatus of the source Task
	 * 
	 * @return The new TaskStatus
	 */
	public TaskStatus getStatus() {
		return status;
	}

}
