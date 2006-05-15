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

package net.sf.mzmine.io;

import java.io.File;
import java.io.IOException;


import net.sf.mzmine.io.RawDataFile.PreloadLevel;
import net.sf.mzmine.io.mzxml.MZXMLFileOpeningTask;
import net.sf.mzmine.io.mzxml.MZXMLFileWriter;
import net.sf.mzmine.io.netcdf.NetCDFFileOpeningTask;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.util.Logger;

/**
 * IO controller
 *
 */
public class IOController implements TaskListener {

    private static IOController myInstance;

    private enum FileType { MZXML, NETCDF, UNKNOWN };

    public IOController() {
        assert myInstance == null;
        myInstance = this;
    }

    public static IOController getInstance() {
        return myInstance;
    }

    /**
     * This method is non-blocking, it places a request to open these files and
     * exits immediately.
     */
    public void openFiles(File[] files, PreloadLevel preloadLevel) {

        String extension;
        Task openTask;

        for (File file : files) {

			FileType fileType = determineFileType(file);

			switch(fileType) {
				case MZXML:
					openTask = new MZXMLFileOpeningTask(file, preloadLevel);
					TaskController.getInstance().addTask(openTask, this);
					break;
				case NETCDF:
					openTask = new NetCDFFileOpeningTask(file, preloadLevel);
					TaskController.getInstance().addTask(openTask, this);
					break;
				case UNKNOWN:
				default:
	                MainWindow.getInstance().displayErrorMessage("Unknown file format of file " + file);
					break;
			}

        }

    }

    /**
     * TODO: Currently always uses MZXMLFileWriter, should also include NetCDFWriter when it is implemented
     *
     */
    public RawDataFileWriter createNewTemporaryFile(RawDataFile file) throws IOException {


		// Determine file type
		FileType fileType = determineFileType(file.getOriginalFile());
		if (fileType==FileType.UNKNOWN) { return null; }


		// Create new temp file
		File workingCopy;
		try {
			workingCopy = File.createTempFile("MZmine", null);
			workingCopy.deleteOnExit();
		} catch (SecurityException e) {
			Logger.putFatal("Could not prepare newly created temporary copy for deletion on exit.");
			throw new IOException("Could not prepare newly created temporary copy for deletion on exit.");
		}


		// Create the writer
		switch(fileType) {
			case MZXML:
			case NETCDF:
				RawDataFileWriter rawDataFileWriter = new MZXMLFileWriter(file, workingCopy, file.getPreloadLevel());
				return rawDataFileWriter;

			default:
				return null;

		}

    }

    private FileType determineFileType(File file) {

		String extension;

		extension = file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase();
		if (extension.endsWith("xml")) { return FileType.MZXML; }
		if (extension.equals("cdf")) { return FileType.NETCDF; }
		return FileType.UNKNOWN;

	}

    /**
     * This method is called when the file opening task is finished.
     *
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

            RawDataFile newFile = (RawDataFile) task.getResult();
            MZmineProject.getCurrentProject().addFile(newFile);

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            Logger.putFatal("Error opening a file: " + task.getErrorMessage());
            MainWindow.getInstance().displayErrorMessage(
                    "Error opening a file: " + task.getErrorMessage());

        }
    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskStarted(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskStarted(Task task) {
        // do nothing
    }
}
