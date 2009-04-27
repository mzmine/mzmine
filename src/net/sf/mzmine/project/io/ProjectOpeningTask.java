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
package net.sf.mzmine.project.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;

public class ProjectOpeningTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;
	private File openFile;
	private ZipInputStream zipStream;
	private RawDataFileOpen rawDataFileOpen;
	private PeakListOpen peakListOpen;
	private ZipFile zipFile;
	private String fileName;
	private int currentStage;
	
	public ProjectOpeningTask(File openFile) {
		this.openFile = openFile;
		try {
			zipFile = new ZipFile(openFile);
			rawDataFileOpen = new RawDataFileOpen();
		} catch (IOException ex) {
			Logger.getLogger(ProjectOpeningTask.class.getName()).log(Level.SEVERE, null, ex);
		}
		removeCurrentProjectFiles();
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {		
		return "Opening project " + fileName;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		switch (currentStage) {
			case 1:
				try {
					return (double) rawDataFileOpen.getProgress();
				} catch (Exception e) {
					return 0f;
				}
			case 2:
				try {
					return (double) peakListOpen.getProgress();
				} catch (Exception e) {
					return 0f;
				}
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

			// Read the project ZIP file
			for (int i = 0; i < this.zipFile.size(); i++) {
				
				ZipEntry entry = zipStream.getNextEntry();
				fileName = entry.getName();

				if (entry.getName().equals("configuration.xml")) {

					currentStage = 0;
					loadProjectInformation(zipFile.getInputStream(entry));

				} else if (entry.getName().matches("Raw data file #.*")) {

					currentStage = 1;
					try {
						rawDataFileOpen = new RawDataFileOpen();
						rawDataFileOpen.readRawDataFile(zipFile, entry, zipStream);
						i++;
					} catch (Exception ex) {
						MZmineCore.getDesktop().displayErrorMessage("Error loading raw data file: " + entry.getName());
						Logger.getLogger(ProjectOpeningTask.class.getName()).log(Level.SEVERE, null, ex);
					}

				} else if (entry.getName().matches("Peak list #.*")) {

					currentStage = 2;
					try {
						peakListOpen = new PeakListOpen();
						peakListOpen.readPeakList(zipFile, entry);
					} catch (Exception ex) {
						MZmineCore.getDesktop().displayErrorMessage("Error loading peak list file: " + entry.getName());
						Logger.getLogger(ProjectOpeningTask.class.getName()).log(Level.SEVERE, null, ex);
					}

				}
			}

			// Finish and close the project ZIP file
			zipStream.close();

			// Final check for cancel
			if (status == TaskStatus.CANCELED) {
				return;
			}
			((MZmineProjectImpl) MZmineCore.getCurrentProject()).setProjectFile(openFile);

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

	/**
	 * Load the configuration file from the project zip file
	 */
	private void loadProjectInformation(InputStream inputStream) throws IOException {
		logger.info("Loading configuration file");

		File tempConfigFile = File.createTempFile("mzmineconfig", ".tmp");
		FileOutputStream fileStream = new FileOutputStream(tempConfigFile);
		(new SaveFileUtils()).saveFile(inputStream, fileStream, 0, SaveFileUtilsMode.CLOSE_OUT);
		fileStream.close();

		MZmineCore.loadConfiguration(tempConfigFile);

		tempConfigFile.delete();
	}

	
	/**
	 * Remove the raw data files and the peak lists of the current project
	 */
	public void removeCurrentProjectFiles() {
		MZmineProject project = MZmineCore.getCurrentProject();		
		for (RawDataFile file : project.getDataFiles()) {
			project.removeFile(file);
		}
		for (PeakList peakList : project.getPeakLists()) {
			project.removePeakList(peakList);
		}
	}

	public Object[] getCreatedObjects() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
