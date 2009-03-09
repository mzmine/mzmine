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
import java.io.ObjectOutputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.project.impl.xstream.MZmineXStream;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.UnclosableOutputStream;

/**
 * Project saving task using XStream library
 */
public class ProjectSavingTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;
	private File saveFile;
	private MZmineProjectImpl project;
	private ZipOutputStream zipStream;
	private UnclosableOutputStream unclosableZipStream;
	private MZmineXStream xstream;
	private int currentStage;
	private StoredProjectDescription description;

	public ProjectSavingTask(File saveFile) {
		this.saveFile = saveFile;
		xstream = new MZmineXStream();
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		String taskDescription = "Saving project to " + saveFile;
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
					return (double) xstream.getNumOfSerializedRows() / description.getNumOfPeakListRows();
				}
			case 4:
				return 1;
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
			File tempFile = File.createTempFile("mzmineproject", ".tmp");
			FileOutputStream tempStream = new FileOutputStream(tempFile);
			zipStream = new ZipOutputStream(tempStream);
			unclosableZipStream = new UnclosableOutputStream(zipStream);


			// Stage 1 - save project description
			currentStage++;
			saveProjectDescription();
			if (status == TaskStatus.CANCELED) {
				return;
			}

			// Stage 2 - save configuration
			currentStage++;
			saveConfiguration();
			if (status == TaskStatus.CANCELED) {
				return;
			}

			// Stage 3 - save RawDataFile objects
			currentStage++;
			saveRawDataObjects();
			if (status == TaskStatus.CANCELED) {
				return;
			}

			// Stage 4 - save PeakList objects
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

	private void saveRawDataObjects() throws IOException {
		zipStream.putNextEntry(new ZipEntry("rawDataFiles.xml"));

		// Create object stream
		ObjectOutputStream objectStream = xstream.createObjectOutputStream(unclosableZipStream);

		// Serialize datafiles
		for (RawDataFile dataFile : project.getDataFiles()) {
			if (status == TaskStatus.CANCELED) {
				return;
			}
			objectStream.writeObject(dataFile.getPath());
		}
		objectStream.close();
	}

	private void savePeakListObjects() throws IOException {
		zipStream.putNextEntry(new ZipEntry("peakLists.xml"));

		ObjectOutputStream objectStream = xstream.createObjectOutputStream(unclosableZipStream);

		// Write all peak list rows
		for (PeakList peakList : project.getPeakLists()) {

			RawDataFile[] data = peakList.getRawDataFiles();
			String[] rawDataNames = new String[data.length];
			for (int i = 0; i < data.length; i++) {
				rawDataNames[i] = data[i].getName();
			}
			StoredPeakListDescription peakListDescription = new StoredPeakListDescription(peakList.getName(),
					rawDataNames, peakList.getNumberOfRows());
			objectStream.writeObject(peakListDescription);

			// Write rows
			for (PeakListRow row : peakList.getRows()) {
				objectStream.writeInt(row.getID());
				objectStream.writeInt(row.getNumberOfPeaks());

				// Write peaks
				for (ChromatographicPeak peak : row.getPeaks()) {
					StoredChromatographicPeakDescription storedPeak = new StoredChromatographicPeakDescription(peak);
					objectStream.writeObject(storedPeak);

				}
			}

		}
		objectStream.close();
	}


	private void saveProjectDescription() throws IOException {
		description = new StoredProjectDescription(project);
		zipStream.putNextEntry(new ZipEntry("info.xml"));
		ObjectOutputStream objectStream = xstream.createObjectOutputStream(unclosableZipStream);
		objectStream.writeObject(description);
		objectStream.close();
	}

	private void saveConfiguration() throws IOException {
		zipStream.putNextEntry(new ZipEntry("config.xml"));
		File tempConfigFile = File.createTempFile("mzmineconfig", ".tmp");
		MZmineCore.saveConfiguration(tempConfigFile);
		FileInputStream configInputStream = new FileInputStream(tempConfigFile);
		byte buffer[] = new byte[1 << 10]; // 1 MB buffer
		int len;
		while ((len = configInputStream.read(buffer)) > 0) {
			zipStream.write(buffer, 0, len);
		}
		configInputStream.close();
		tempConfigFile.delete();
	}
}
