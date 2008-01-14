/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.taskcontrol;

import java.util.logging.Logger;

import net.sf.mzmine.main.MZmineCore;

/**
 * 
 */
public class TaskGroup implements TaskListener {

    public enum TaskGroupStatus {
        WAITING, RUNNING, ERROR, CANCELED, FINISHED
    };

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private Task tasks[];
    private TaskListener taskListener;
    private TaskGroupListener taskGroupListener;
    private int finishedTasks = 0;

    private TaskGroupStatus status = TaskGroupStatus.WAITING;

    /**
     * @param tasks
     * @param taskController
     */
    public TaskGroup(Task task, TaskListener taskListener) {
        this(new Task[] { task }, taskListener, null);
    }
    
    /**
     * @param tasks
     * @param taskController
     */
    public TaskGroup(Task[] tasks, TaskListener taskListener) {
        this(tasks, taskListener, null);
    }

    /**
     * @param tasks
     * @param taskController
     */
    public TaskGroup(Task task, TaskListener taskListener,
            TaskGroupListener groupListener) {
        this(new Task[] { task }, taskListener, groupListener);
    }
    
    /**
     * @param tasks
     * @param taskController
     */
    public TaskGroup(Task[] tasks, TaskListener taskListener,
            TaskGroupListener groupListener) {
        this.tasks = tasks;
        this.taskListener = taskListener;
        this.taskGroupListener = groupListener;
    }

    public TaskGroupStatus getStatus() {
        return status;
    }

    public void taskStarted(Task task) {
        if (taskListener != null)
            taskListener.taskStarted(task);
    }

    public synchronized void taskFinished(Task task) {

        if (taskListener != null)
            taskListener.taskFinished(task);

        if (task.getStatus() == Task.TaskStatus.ERROR) {
            status = TaskGroupStatus.ERROR;
        } else if (task.getStatus() == Task.TaskStatus.CANCELED) {
            status = TaskGroupStatus.CANCELED;
        }

        finishedTasks++;

        if (finishedTasks == tasks.length) {

            if (status == TaskGroupStatus.RUNNING)
                status = TaskGroupStatus.FINISHED;

            if (taskGroupListener != null)
                taskGroupListener.taskGroupFinished(this);

        }

        logger.finest("Task group: finished " + finishedTasks + "/"
                + tasks.length + " tasks, status " + status);

    }

    /**
     */
    public void start() {

        logger.finest("Starting " + tasks.length + " task group");

        status = TaskGroupStatus.RUNNING;

        for (Task t : tasks) {
            MZmineCore.getTaskController().addTask(t, this);
        }

    }

    public String toString() {
        return "Task group: " + tasks;
    }

}
