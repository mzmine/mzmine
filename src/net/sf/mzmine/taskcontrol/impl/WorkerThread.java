/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.taskcontrol.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;

/**
 * Task controller worker thread
 */
class WorkerThread extends Thread {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    private WrappedTask currentTask;
    private Desktop desktop;

    WorkerThread(int workerNumber, Desktop desktop) {
        super("Worker thread #" + workerNumber);
        this.desktop = desktop;
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
                
            } catch (Throwable e) {

                // this should never happen!

                logger.log(Level.SEVERE, "Unhandled exception while processing task "
                        + currentTask, e);

                if (desktop != null) {
                    
                    String errorMessage = "Unhandled exception while processing task "
                        + currentTask + ": " + e;

                    desktop.displayErrorMessage(errorMessage);
                }

            }

            /* discard the task, so that garbage collecter can collect it */
            currentTask = null;

        }

    }
    
    public String toString() {
        return this.getName();
    }

}
