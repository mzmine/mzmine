/*
 * Copyright 2006-2007 The MZmine Development Team
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

/**
 * 
 */
public class TaskSequence implements TaskListener, Runnable {

    public enum TaskSequenceStatus {
        WAITING, RUNNING, ERROR, CANCELED, FINISHED
    };

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private Task tasks[];
    private TaskListener taskListener;
    private TaskSequenceListener sequenceListener;
    private TaskController taskController;
    private int finishedTasks = 0;

    private TaskSequenceStatus status = TaskSequenceStatus.WAITING;

    /**
     * @param tasks
     * @param taskController
     */
    public TaskSequence(Task[] tasks, TaskListener taskListener,
            TaskController taskController) {
        this(tasks, taskListener, null, taskController);
    }

    /**
     * @param tasks
     * @param taskController
     */
    public TaskSequence(Task[] tasks, TaskListener taskListener,
            TaskSequenceListener sequenceListener, TaskController taskController) {

        this.tasks = tasks;
        this.taskListener = taskListener;
        this.sequenceListener = sequenceListener;
        this.taskController = taskController;

    }

    public TaskSequenceStatus getStatus() {
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
            status = TaskSequenceStatus.ERROR;
        } else if (task.getStatus() == Task.TaskStatus.CANCELED) {
            status = TaskSequenceStatus.CANCELED;
        }

        finishedTasks++;

        if (finishedTasks == tasks.length) {

            logger.finest("Task sequence finished");

            if (status == TaskSequenceStatus.RUNNING)
                status = TaskSequenceStatus.FINISHED;

            if (sequenceListener != null)
                sequenceListener.taskSequenceFinished(this);

        }

    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        logger.finest("Running " + tasks.length + " task sequence");

        status = TaskSequenceStatus.RUNNING;

        for (Task t : tasks) {
            taskController.addTask(t, this);
        }

    }
    
    public String toString() {
        return "Task sequence: " + tasks;
    }

}
