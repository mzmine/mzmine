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

package net.sf.mzmine.taskcontrol;

import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.Logger;

/**
 * 
 */
class WorkerThread extends Thread {

    private Task currentTask;

    WorkerThread(int workerNumber) {
        super("Task controller worker thread #" + workerNumber);
    }

    /**
     * @return Returns the currentTask.
     */
    Task getCurrentTask() {
        return currentTask;
    }

    /**
     * @param currentTask
     *            The currentTask to set.
     */
    void setCurrentTask(Task newTask) {
        assert currentTask == null;
        currentTask = newTask;
        synchronized(this) {
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
                    synchronized(this) {
                        wait();
                    }
                } catch (InterruptedException e) {
                    // nothing happens
                }
            }

            switch (currentTask.getPriority()) {
            case HIGH:
                setPriority(MAX_PRIORITY);
                break;
            case LOW:
                setPriority(MIN_PRIORITY);
                break;
            default:
                setPriority(NORM_PRIORITY);
                break;
            }


            try {
                currentTask.run();
            } catch (Throwable e) {
                
                // this should never happen!
                
                String errorMessage = "Unhandled exception while processing task "
                    + currentTask.getTaskDescription() + ": " + e + ", cancelling the task.";
                Logger.putFatal(errorMessage);
                
                currentTask.cancel();
                
                if (MainWindow.getInstance() != null)
                    MainWindow.getInstance().displayErrorMessage(errorMessage);
                
            }
            
            /* discard the task, so that garbage collecter can collect it */
            currentTask = null;
            
        }

    }

}
