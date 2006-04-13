/*
 * Copyright 2006 Okinawa Institute of Science and Technology
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

package net.sf.mzmine.io;

import java.io.File;

import net.sf.mzmine.io.mzxml.MZXMLFileOpeningTask;
import net.sf.mzmine.io.netcdf.NetCDFFileOpeningTask;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;

/**
 * C Files may be opened in following ways: - on the client, user selects "Open
 * file" from menu, new task is created and then the file appears in open files
 * list
 * 
 */
public class IOController implements TaskListener {

    private static IOController myInstance;

    public IOController() {
        assert myInstance == null;
        myInstance = this;
    }
    
    /**
     * This method is non-blocking, it places a request to open these files and
     * exits immediately.
     */
    public void openFiles(File[] files) {
        String extension;
        Task openTask;

        for (File file : files) {
            /* TODO: determine file contents by header */
            extension = file.getName().substring(
                    file.getName().lastIndexOf(".") + 1).toLowerCase();
            if (extension.endsWith("mzxml")) {
                openTask = new MZXMLFileOpeningTask(file);
                TaskController.getInstance().addTask(openTask, this);
            } else if (extension.equals("cdf")) {
                openTask = new NetCDFFileOpeningTask(file);
                TaskController.getInstance().addTask(openTask, this);
            }

        }

    }

    public static IOController getInstance() { return myInstance; }
    
    public RawDataFileWriter createNewTemporaryFile(RawDataFile file) {
        return null;
    }



    /**
     * This method is called when the file opening task is finished.
     * 
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {
        // TODO Auto-generated method stub

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

            try {

                RawDataFile newFile = (RawDataFile) task.getResult();
                MZmineFile newMZFile = new MZmineFile(newFile);
                // TODO:
                // MainWindow.getInstance().getFileListComponent().addFile(
                // newMZFile);
            } catch (ClassCastException e) {

            }
        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            // TODO: show a message box Task.get
        }
    }
}
