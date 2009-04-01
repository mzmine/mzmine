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
package net.sf.mzmine.project.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.util.ExceptionUtils;

/**
 * Project opening task using XStream library
 */
public class ProjectOpeningTask implements Task, TaskListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;
	private File openFile;
	private ZipInputStream zipStream;
	// private UnclosableInputStream unclosableZipStream;
	private StoredProjectDescription description;
	private int currentStage;

	public ProjectOpeningTask(File openFile) {
		this.openFile = openFile;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		String taskDescription = "Opening project " + openFile;
		switch (currentStage) {
			case 2:
				return taskDescription + " (raw data points)";
			case 3:
				return taskDescription + " (peak list objects)";
			default:
				return taskDescription;
		}

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		switch (currentStage) {
			case 3:
				if (description.getNumOfPeakListRows() == 0) {
					return 0;
				} else {
					//return (double) xstream.getNumOfDeserializedRows() / description.getNumOfPeakListRows();
				}
			case 4:
				return 1f;
			default:
				return 0f;
		}
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getStatus()
	 */
	public TaskStatus getStatus() {
		return status;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		try {

			logger.info("Started opening project " + openFile);
			status = TaskStatus.PROCESSING;


			// Get project ZIP stream
			FileInputStream fileStream = new FileInputStream(openFile);
			zipStream = new ZipInputStream(fileStream);
			// unclosableZipStream = new UnclosableInputStream(zipStream);

			//remove previous data
			/*MZmineProject project = MZmineCore.getCurrentProject();
			for (RawDataFile data : project.getDataFiles()) {
			project.removeFile(data);
			}
			for (PeakList data : project.getPeakLists()) {
			project.removePeakList(data);
			}*/

			// Stage 1 - load project description
			currentStage++;
			description = loadProjectDescription();

			// Stage 2 - load configuration
			currentStage++;
			loadConfiguration();

			// Stage 3 - load RawDataFile objects
			currentStage++;
			loadRawDataObjects();

			// Stage 4 - load PeakList objects
			currentStage++;
			loadPeakListObjects();


			// Finish and close the project ZIP file
			zipStream.close();

			// Final check for cancel
			if (status == TaskStatus.CANCELED) {
				return;
			}
			//	((MZmineProjectImpl) project).setProjectFile(openFile);

			logger.info("Finished opening project " + openFile);
			status = TaskStatus.FINISHED;

		} catch (Throwable e) {
			status = TaskStatus.ERROR;
			errorMessage = "Failed opening project: " + ExceptionUtils.exceptionToString(e);
		}
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
	public void cancel() {
		logger.info("Canceling opening of project " + openFile);
		status = TaskStatus.CANCELED;
	}

	private StoredProjectDescription loadProjectDescription()
			throws IOException, ClassNotFoundException {
		zipStream.getNextEntry();
		ObjectInputStream in = new ObjectInputStream(zipStream);
		StoredProjectDescription obj = (StoredProjectDescription) in.readObject();		
		return obj;
	}

	private void loadConfiguration() throws IOException {
		zipStream.getNextEntry();
		File tempConfigFile = File.createTempFile("mzmineconfig", ".tmp");
		FileOutputStream fileStream = new FileOutputStream(tempConfigFile);
		int len;
		byte buffer[] = new byte[1 << 10]; // 1 MB buffer
		while ((len = zipStream.read(buffer)) > 0) {
			if (status == TaskStatus.CANCELED) {
				return;
			}
			fileStream.write(buffer, 0, len);
		}
		fileStream.close();
		MZmineCore.loadConfiguration(tempConfigFile);
		tempConfigFile.delete();
	}

	private void loadRawDataObjects() throws IOException,
			ClassNotFoundException {		
		for (int i = 0; i < description.getNumOfDataFiles(); i++) {
			ZipEntry entry = zipStream.getNextEntry();
			RawDataFileWriter rawDataFileWriter = MZmineCore.createNewFile(entry.getName().substring(0, entry.getName().lastIndexOf("-")));
			File tempConfigFile = File.createTempFile("mzmine", ".scans");
			FileOutputStream fileStream = new FileOutputStream(tempConfigFile);
			ByteBuffer buffer = ByteBuffer.allocate((int)entry.getSize());

			int len;
			//byte buffer[] = new byte[1 << 10]; // 1 MB buffer
			while ((len = zipStream.read(buffer.array())) > 0) {
				if (status == TaskStatus.CANCELED) {
					return;
				}
				DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();
				for(int j = 0; j < doubleBuffer.capacity(); j++){
					DataPoint dp = new SimpleDataPoint(doubleBuffer.get(j),doubleBuffer.get(j++));
				}


				//fileStream.write(buffer, 0, len);
			}





			//DataPoint[]
			fileStream.close();	
		    
           
			RawDataFile filteredRawDataFile = rawDataFileWriter.finishWriting();
            MZmineCore.getCurrentProject().addFile(filteredRawDataFile);
		}
	}

	private void loadPeakListObjects() throws IOException,
			ClassNotFoundException {
		zipStream.getNextEntry();

	}

	public void taskStarted(Task task) {
		Task openTask = task;
		logger.info("Started action of " + openTask.getTaskDescription());
	}

	public void taskFinished(Task task) {
		if (task.getStatus() == Task.TaskStatus.FINISHED) {
			logger.info("Finished action of " + task.getTaskDescription());
		}

		if (task.getStatus() == Task.TaskStatus.ERROR) {
			String msg = "Error while trying to " + task.getTaskDescription() + ": " + task.getErrorMessage();
			logger.severe(msg);
		}
	}
}
