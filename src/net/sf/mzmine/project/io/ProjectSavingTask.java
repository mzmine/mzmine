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

import net.sf.mzmine.project.impl.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.zip.ZipOutputStream;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;


public class ProjectSavingTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;
	private File saveFile;
	private MZmineProjectImpl project;
	private ZipOutputStream zipStream;
	private int currentStage;
	private File tempFile;
	private RawDataFileSerializer rawDataFileSerializer;
	private PeakListSerializer peakListSerializer;
	private String rawDataName, peakListName;


	public ProjectSavingTask(File saveFile) {
		this.saveFile = saveFile;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		String taskDescription = "Saving project ";
		switch (currentStage) {
			case 1:
				return taskDescription + "(project information)";
			case 2:
				return taskDescription + "(raw data points) " + rawDataName;
			case 3:
				return taskDescription + "(peak list objects) " + peakListName;
			default:
				return taskDescription;
		}

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {

		switch (currentStage) {			
			case 2:
				try {
					return (double) rawDataFileSerializer.getProgress();
				} catch (Exception e) {
					return 0f;
				}

			case 3:
				try {
					return (double) rawDataFileSerializer.getProgress();
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
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
	public void cancel() {
		logger.info("Canceling saving of project to " + saveFile);
		status = TaskStatus.CANCELED;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		try {

			logger.info("Started saving project to " + saveFile);
			status = TaskStatus.PROCESSING;

			// Get project data
			project = (MZmineProjectImpl) MZmineCore.getCurrentProject();

			// Prepare a temporary ZIP file
			tempFile = File.createTempFile("mzmineproject", ".tmp");
			FileOutputStream tempStream = new FileOutputStream(tempFile);
			zipStream = new ZipOutputStream(tempStream);

			// Stage 1 - save configuration
			currentStage++;
			saveConfiguration();
			if (status == TaskStatus.CANCELED) {
				return;
			}

			// Stage 2 - save RawDataFile objects
			currentStage++;
			saveRawDataObjects();
			if (status == TaskStatus.CANCELED) {
				return;
			}

			// Stage 3 - save PeakList objects
			currentStage++;
			savePeakListObjects();
			if (status == TaskStatus.CANCELED) {
				return;
			}

			// Finish and close the temporary ZIP file
			zipStream.close();

			// Final check for cancel
			if (status == TaskStatus.CANCELED) {
				return;
			}

			// Move the temporary ZIP file to the final location
			tempFile.renameTo(saveFile);

			logger.info("Finished saving project to " + saveFile);
			status = TaskStatus.FINISHED;

		} catch (Throwable e) {
			status = TaskStatus.ERROR;
			errorMessage = "Failed saving the project: " + ExceptionUtils.exceptionToString(e);
		}
	}



	private void saveConfiguration() throws IOException {
		ProjectSerializer projectSerializer = new ProjectSerializer(zipStream);
		projectSerializer.saveProjectDescription(project);
		projectSerializer.saveConfiguration();
	}

	private void saveRawDataObjects() {
		rawDataFileSerializer = new RawDataFileSerializer(zipStream);
		for (RawDataFile rawDataFile : project.getDataFiles()) {
			rawDataName = rawDataFile.getName();
			rawDataFileSerializer.writeRawDataFiles(rawDataFile);			
		}
	}

	private void savePeakListObjects() throws IOException, Exception {
		peakListSerializer = new PeakListSerializer(zipStream);
		for (PeakList peakList : project.getPeakLists()) {
			peakListName = peakList.getName();
			peakListSerializer.savePeakList(peakList);			
		}
	}

	public Object[] getCreatedObjects() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
