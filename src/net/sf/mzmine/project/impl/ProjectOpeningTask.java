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

import com.sun.java.ExampleFileFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;


import javax.swing.JFileChooser;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.desktop.impl.DesktopParameters;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.modules.io.rawdataimport.fileformats.MzDataReadTask;
import net.sf.mzmine.modules.io.rawdataimport.fileformats.MzMLReadTask;
import net.sf.mzmine.modules.io.rawdataimport.fileformats.MzXMLReadTask;
import net.sf.mzmine.modules.io.rawdataimport.fileformats.NetCDFReadTask;
import net.sf.mzmine.modules.io.rawdataimport.fileformats.XcaliburRawFileReadTask;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.impl.xstream.MZmineXStream;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroup.TaskGroupStatus;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.UnclosableInputStream;

/**
 * Project opening task using XStream library
 */
public class ProjectOpeningTask implements Task, TaskListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;
	private MZmineXStream xstream;
	private File openFile;
	private ZipInputStream zipStream;
	private UnclosableInputStream unclosableZipStream;
	private StoredProjectDescription description;
	private int currentStage;

	public ProjectOpeningTask(File openFile) {
		this.openFile = openFile;
		xstream = new MZmineXStream();
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
					return (double) xstream.getNumOfDeserializedRows() / description.getNumOfPeakListRows();
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
			unclosableZipStream = new UnclosableInputStream(zipStream);

			//remove previous data
			MZmineProject project = MZmineCore.getCurrentProject();
			for (RawDataFile data : project.getDataFiles()) {
				project.removeFile(data);
			}
			for (PeakList data : project.getPeakLists()) {
				project.removePeakList(data);
			}

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
			((MZmineProjectImpl)project).setProjectFile(openFile);
			
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
		ObjectInputStream objectStream = xstream.createObjectInputStream(unclosableZipStream);
		StoredProjectDescription projectDescription = (StoredProjectDescription) objectStream.readObject();
		objectStream.close();
		return projectDescription;
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
		zipStream.getNextEntry();
		ObjectInputStream objectStream = xstream.createObjectInputStream(unclosableZipStream);
		File[] file = new File[description.getNumOfDataFiles()];
		for (int i = 0; i < description.getNumOfDataFiles(); i++) {
			file[i] = new File((String) objectStream.readObject());
			if(!file[i].exists()){
				this.getNewPath();
			}
		}
		objectStream.close();

		this.openRawData(file);
	}


	private void getNewPath(){
		DesktopParameters parameters = (DesktopParameters) MZmineCore
				.getDesktop().getParameterSet();
		String lastPath = parameters.getLastOpenProjectPath();
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogType(JFileChooser.DIRECTORIES_ONLY);
		if (lastPath != null)
			chooser.setCurrentDirectory(new File(lastPath));

		int returnVal = chooser.showOpenDialog(MZmineCore.getDesktop()
				.getMainFrame());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String selectedFile = chooser.getSelectedFile().getPath();
			
		}
	}

	private void openRawData(File[] file) {
		Task openTasks[] = new Task[file.length];

		for (int i = 0; i < file.length; i++) {
			String extension = file[i].getName().substring(
					file[i].getName().lastIndexOf(".") + 1).toLowerCase();

			if (extension.endsWith("mzdata")) {
				openTasks[i] = new MzDataReadTask(file[i]);
			}
			if (extension.endsWith("mzxml")) {
				openTasks[i] = new MzXMLReadTask(file[i]);
			}
			if (extension.endsWith("mzml")) {
				openTasks[i] = new MzMLReadTask(file[i]);
			}
			if (extension.endsWith("cdf")) {
				openTasks[i] = new NetCDFReadTask(file[i]);
			}
			if (extension.endsWith("raw")) {
				openTasks[i] = new XcaliburRawFileReadTask(file[i]);
			}
			if (openTasks[i] == null) {
				logger.finest("Cannot determine file type of file " + file[i]);
			}
		}
		TaskGroup newGroup = new TaskGroup(openTasks, this, null);

		// start this group
		newGroup.start();
		while (newGroup.getStatus() != TaskGroupStatus.FINISHED) {
			//System.out.println(newGroup.getStatus());
		}
	}

	private synchronized void loadPeakListObjects() throws IOException,
			ClassNotFoundException {
		zipStream.getNextEntry();
		ObjectInputStream objectStream = xstream.createObjectInputStream(unclosableZipStream);
		RawDataFile[] dataFiles = MZmineCore.getCurrentProject().getDataFiles();

		for (int i = 0; i < description.getNumOfPeakLists(); i++) {

			StoredPeakListDescription peakListDescription = (StoredPeakListDescription) objectStream.readObject();
			// Create new peak list
			SimplePeakList newPeakList = new SimplePeakList(
					peakListDescription.getName(), this.getDataFilesByName(dataFiles, peakListDescription.getRawDataFiles()));

			//Create rows
			int numberRows = peakListDescription.getNumberRows();

			for (int j = 0; j < numberRows; j++) {
				int rowId = objectStream.readInt();
				PeakListRow row = new SimplePeakListRow(rowId);
				int numberPeaks = objectStream.readInt();
				for (int e = 0; e < numberPeaks; e++) {
					StoredChromatographicPeakDescription storedPeak = (StoredChromatographicPeakDescription) objectStream.readObject();
					String dataFileName = storedPeak.getDataFileName();
					RawDataFile rawData = this.getDataFileByName(dataFiles, dataFileName);
					if (rawData != null) {
						row.addPeak(rawData, storedPeak.getPeak());
					}
				}
				newPeakList.addRow(row);
			}
			MZmineCore.getCurrentProject().addPeakList(newPeakList);
		}
		objectStream.close();
	}

	private RawDataFile[] getDataFilesByName(RawDataFile[] dataFiles, String[] rawDataNames) {
		RawDataFile[] realDataFiles = new RawDataFile[rawDataNames.length];
		int cont = 0;
		for (String rawDataName : rawDataNames) {
			for (RawDataFile file : dataFiles) {
				if (rawDataName.compareTo(file.getName()) == 0) {
					realDataFiles[cont++] = file;
					break;
				}
			}
		}
		return realDataFiles;
	}

	private RawDataFile getDataFileByName(RawDataFile[] dataFiles, String rawDataNames) {

		for (RawDataFile file : dataFiles) {
			if (rawDataNames.compareTo(file.getName()) == 0) {
				return file;
			}
		}

		return null;
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
