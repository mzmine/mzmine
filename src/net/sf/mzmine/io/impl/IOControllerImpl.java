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

package net.sf.mzmine.io.impl;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import net.sf.mzmine.io.IOController;
import net.sf.mzmine.io.PreloadLevel;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;

/**
 * IO controller
 */
public class IOControllerImpl implements IOController, TaskListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private TaskController taskController;
    private Desktop desktop;

    /**
     * This method is non-blocking, it places a request to open these files and
     * exits immediately.
     */
    public void openFiles(File[] files, PreloadLevel preloadLevel) {

        Task openTask;

        for (File file : files) {

            openTask = new FileOpeningTask(file, preloadLevel);
            taskController.addTask(openTask, this);
        }

    }

    /**
     * This method is called when the file opening task is finished.
     * 
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

            RawDataFile newFile = (RawDataFile) task.getResult();
            MZmineCore.getCurrentProject().addFile(newFile);

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            logger.severe("Error opening a file: " + task.getErrorMessage());
            desktop.displayErrorMessage("Error opening a file: "
                    + task.getErrorMessage());

        }
    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskStarted(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskStarted(Task task) {
        // do nothing
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {
        this.taskController = MZmineCore.getTaskController();
        this.desktop = MZmineCore.getDesktop();
    }

    /**
     * @see net.sf.mzmine.io.IOController#createNewFile(java.lang.String,
     *      net.sf.mzmine.io.PreloadLevel)
     */
    public RawDataFile createNewFile(String name,
            PreloadLevel preloadLevel) throws IOException {
        return new RawDataFileImpl(name, preloadLevel);
    }

}
