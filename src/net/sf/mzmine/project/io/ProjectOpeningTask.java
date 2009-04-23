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
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
	private ProjectOpen projectOpen;
	private RawDataFileOpen rawDataFileOpen;
	private PeakListOpen peakListOpen;
	private ZipFile zipFile;
	private int currentStage,  rawDataCount,  peakListCount;

	public ProjectOpeningTask(File openFile) {
		this.openFile = openFile;
		try {
			zipFile = new ZipFile(openFile);
		} catch (IOException ex) {
			Logger.getLogger(ProjectOpeningTask.class.getName()).log(Level.SEVERE, null, ex);
		}
		removeCurrentProjectFiles();
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		String taskDescription = "Opening project ";
		switch (currentStage) {
			case 2:
				String rawDataName = "";
				try {
					rawDataName = this.projectOpen.getRawDataNames()[rawDataCount];
				} catch (Exception e) {
					return taskDescription + "(raw data points) ";
				}
				return taskDescription + "(raw data points) " + rawDataName;
			case 3:
				String peakListName = "";
				try {
					peakListName = this.projectOpen.getPeakListNames()[peakListCount];
				} catch (Exception e) {
					return taskDescription + "(peak list objects)";
				}
				return taskDescription + "(peak list objects)" + peakListName;
			default:
				return taskDescription + openFile;
		}

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		switch (currentStage) {
			case 2:
				try {
					return (double) rawDataFileOpen.getProgress();
				} catch (Exception e) {
					return 0f;
				}
			case 3:
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

			// Stage 1 - load project description
			currentStage++;
			loadProjectInformation();

			// Stage 2 - load RawDataFile objects
			currentStage++;
			loadRawDataObjects();

			// Stage 3 - load PeakList objects
			currentStage++;
			loadPeakListObjects();

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
	 * Loads the configuration file and the project information from the project zip file
	 */
	private void loadProjectInformation() {
		projectOpen = new ProjectOpen(this.zipStream, this.zipFile);
		projectOpen.openProjectDescription();
		projectOpen.openConfiguration();
	}

	/**
	 * Loads the raw data files from the project zip file
	 */
	private void loadRawDataObjects() {
		rawDataFileOpen = new RawDataFileOpen(this.zipStream, this.zipFile);
		for (int i = 0; i < this.projectOpen.getNumOfRawDataFiles(); i++, rawDataCount++) {
			if (this.projectOpen.getRawDataNames()[i] != null) {
				try {
					rawDataFileOpen.readRawDataFile(this.projectOpen.getRawDataNames()[i]);
				} catch (Exception ex) {
					MZmineCore.getDesktop().displayErrorMessage("Error loading raw data file: " + this.projectOpen.getRawDataNames()[i]);
					Logger.getLogger(ProjectOpeningTask.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	/**
	 * Loads the peak lists from the project zip file
	 */
	private void loadPeakListObjects() {
		for (int i = 0; i < this.projectOpen.getNumOfPeakLists(); i++, peakListCount++) {
			try {
				logger.info("Loading peak list: " + this.projectOpen.getPeakListNames()[i]);
				peakListOpen = new PeakListOpen(zipStream, this.zipFile);
				peakListOpen.readPeakList();
			} catch (Exception ex) {
				MZmineCore.getDesktop().displayErrorMessage("Error loading peak list file: " + this.projectOpen.getPeakListNames()[i]);
				Logger.getLogger(ProjectOpeningTask.class.getName()).log(Level.SEVERE, null, ex);

			}
		}
	}

	/**
	 * Removes the raw data files and the peak lists of the current project
	 */
	public void removeCurrentProjectFiles() {
		MZmineProject project = MZmineCore.getCurrentProject();
		RawDataFile[] rawDataFiles = project.getDataFiles();
		PeakList[] peakLists = project.getPeakLists();
		for (RawDataFile file : rawDataFiles) {
			project.removeFile(file);
		}
		for (PeakList peakList : peakLists) {
			project.removePeakList(peakList);
		}
	}

	public Object[] getCreatedObjects() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
}
