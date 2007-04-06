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

package net.sf.mzmine.methods.filtering.mean;

import java.util.Hashtable;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.methods.MethodListener;
import net.sf.mzmine.methods.MethodListener.MethodReturnStatus;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;

/**
 * This class represents a single run of mean filter. Run is a sequence of tasks
 * to filter data files.
 */
class MeanFilterRun implements TaskListener, Runnable {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private MeanFilter moduleInstance;
    private ParameterSet parameters;
    private MeanFilterTask tasks[];
    private MethodListener methodListener;
    private Desktop desktop;
    private TaskController taskController;
    private Hashtable<MeanFilterTask, OpenedRawDataFile> taskToFile;
    private int taskCount;

    private MethodReturnStatus returnStatus = null;

    /**
     * @param tasks
     * @param methodListener
     * @param taskController
     */
    MeanFilterRun(MeanFilter moduleInstance, OpenedRawDataFile[] dataFiles,
            ParameterSet parameters, MethodListener methodListener,
            Desktop desktop, TaskController taskController) {

        this.moduleInstance = moduleInstance;
        this.methodListener = methodListener;
        this.desktop = desktop;
        this.taskController = taskController;

        taskToFile = new Hashtable<MeanFilterTask, OpenedRawDataFile>();

        taskCount = dataFiles.length;
        tasks = new MeanFilterTask[taskCount];
        for (int i = 0; i < dataFiles.length; i++) {
            tasks[i] = new MeanFilterTask(dataFiles[i], parameters);
            taskToFile.put(tasks[i], dataFiles[i]);
        }

    }

    public void taskStarted(Task task) {
        // do nothing
    }

    public synchronized void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

            OpenedRawDataFile openedFile = taskToFile.get(task);
            RawDataFile newFile = (RawDataFile) task.getResult();

            openedFile.updateFile(newFile, moduleInstance, parameters);

            if (returnStatus == null)
                returnStatus = MethodReturnStatus.FINISHED;

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            String msg = "Error while filtering a file: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);

            returnStatus = MethodReturnStatus.ERROR;

        } else if (task.getStatus() == Task.TaskStatus.CANCELED) {

            returnStatus = MethodReturnStatus.CANCELED;

        }

        taskCount--;

        if ((taskCount == 0) && (methodListener != null)) {
            methodListener.methodFinished(returnStatus);
        }

    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        logger.info("Running mean filter");

        for (MeanFilterTask filterTask : tasks) {
            taskController.addTask(filterTask, this);
        }

    }

}
