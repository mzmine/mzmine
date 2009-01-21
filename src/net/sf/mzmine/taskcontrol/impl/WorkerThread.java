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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.taskcontrol.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;

/**
 * Task controller worker thread
 */
class WorkerThread extends Thread {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private WrappedTask currentTask;

    WorkerThread(int workerNumber) {
        super("Worker thread #" + workerNumber);
    }

    /**
     * @return Returns the currentTask.
     */
    WrappedTask getCurrentTask() {
        return currentTask;
    }

    /**
     * @param currentTask The currentTask to set.
     */
    void setCurrentTask(WrappedTask newTask) {
        assert currentTask == null;
        currentTask = newTask;
        newTask.assignTo(this);
        synchronized (this) {
            notify();
        }
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        while (true) {

            while (currentTask == null) {
                try {
                    synchronized (this) {
                        wait();
                    }
                } catch (InterruptedException e) {
                    // nothing happens
                }
            }

            try {

                TaskListener listener = currentTask.getListener();

                if (listener != null)
                    listener.taskStarted(currentTask.getTask());

                currentTask.getTask().run();

                if (listener != null)
                    listener.taskFinished(currentTask.getTask());

                // Task finished with an error
                if (currentTask.getTask().getStatus() == TaskStatus.ERROR) {
                    logger.severe("Task error: "
                            + currentTask.getTask().getErrorMessage());
                    if (MZmineCore.getDesktop() != null) {
                        MZmineCore.getDesktop().displayErrorMessage(
                                "Error of task "
                                        + currentTask.getTask().getTaskDescription(),
                                currentTask.getTask().getErrorMessage());
                    }
                }

                /*
                 * This is important to allow the garbage collector to remove
                 * the task, while keeping the task description in the "Tasks in
                 * progress" window
                 */
                currentTask.removeTaskReference();

            } catch (Throwable e) {

                // This should never happen!

                logger.log(Level.SEVERE, "Unhandled exception " + e
                        + " while processing task " + currentTask, e);

                if (MZmineCore.getDesktop() != null) {
                    MZmineCore.getDesktop().displayErrorMessage(
                            "Unhandled exception in task "
                                    + currentTask.getTask().getTaskDescription() + ": " + 
                            ExceptionUtils.exceptionToString(e));
                }

            }

            /*
             * Discard the reference to the task, so the garbage collecter can
             * collect it
             */
            currentTask = null;

        }

    }

    public String toString() {
        return this.getName();
    }

}
