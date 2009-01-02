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

import java.util.Date;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskPriority;

/**
 * Wrapper class for Tasks that stores additional information
 */
class WrappedTask implements Comparable {

    private Task task;
    private Date addedTime;
    private TaskListener listener;
    private TaskPriority priority;
    private WorkerThread assignedTo;

    WrappedTask(Task task, TaskPriority priority, TaskListener listener) {
        addedTime = new Date();
        this.task = task;
        this.listener = listener;
        this.priority = priority;
    }

    /**
     * Tasks are sorted by priority order using this comparator method.
     * 
     * @see java.lang.Comparable#compareTo(T)
     */
    public int compareTo(Object arg) {

        WrappedTask t = (WrappedTask) arg;
        int result;

        result = priority.compareTo(t.priority);
        if (result == 0)
            result = addedTime.compareTo(t.addedTime);
        return result;

    }

    /**
     * @return Returns the listener.
     */
    TaskListener getListener() {
        return listener;
    }

    /**
     * @return Returns the priority.
     */
    TaskPriority getPriority() {
        return priority;
    }

    /**
     * @param priority The priority to set.
     */
    void setPriority(TaskPriority priority) {
        this.priority = priority;
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
    Task getTask() {
        return task;
    }

    public synchronized String toString() {
        return task.getTaskDescription();
    }

    synchronized void removeTaskReference() {
        task = new DummyTask(task);
    }

}
