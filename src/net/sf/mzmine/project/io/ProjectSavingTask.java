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
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.transform.TransformerConfigurationException;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.StreamCopy;

import org.xml.sax.SAXException;

public class ProjectSavingTask implements Task {

	static final String VERSION_FILENAME = "MZMINE_VERSION";
	static final String CONFIG_FILENAME = "configuration.xml";

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;

	private File saveFile;
	private MZmineProjectImpl savedProject;

	private RawDataFileSaveHandler rawDataFileSaveHandler;
	private PeakListSaveHandler peakListSaveHandler;

	private int currentStage;
	private String currentSavedObjectName;

	// This hashtable maps raw data files to their ID within the saved project
	private Hashtable<RawDataFile, Integer> dataFilesIDMap;

	public ProjectSavingTask(File saveFile) {
		this.saveFile = saveFile;
		dataFilesIDMap = new Hashtable<RawDataFile, Integer>();
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		if (currentSavedObjectName == null)
			return "Saving project";
		return "Saving project (" + currentSavedObjectName + ")";
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {

		switch (currentStage) {
		case 2:
			if (rawDataFileSaveHandler == null)
				return 0;
			return rawDataFileSaveHandler.getProgress();
		case 3:
			if (peakListSaveHandler == null)
				return 0;
			return peakListSaveHandler.getProgress();
		case 4:
			return 1;
		default:
			return 0;
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

		if (rawDataFileSaveHandler != null)
			rawDataFileSaveHandler.cancel();

		if (peakListSaveHandler != null)
			peakListSaveHandler.cancel();

	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		try {

			logger.info("Saving project to " + saveFile);
			status = TaskStatus.PROCESSING;

			// Get current project
			savedProject = (MZmineProjectImpl) MZmineCore.getCurrentProject();

			// Prepare a temporary ZIP file. We create this file in the same
			// directory as the final saveFile to avoid moving between
			// filesystems in the last stage (renameTo)
			File tempFile = File.createTempFile(saveFile.getName(), ".tmp",
					saveFile.getParentFile());
			tempFile.deleteOnExit();

			// Create a ZIP stream writing to the temporary file
			FileOutputStream tempStream = new FileOutputStream(tempFile);
			ZipOutputStream zipStream = new ZipOutputStream(tempStream);

			// Stage 1 - save version and configuration
			currentStage++;
			saveVersion(zipStream);
			saveConfiguration(zipStream);
			if (status == TaskStatus.CANCELED)
				return;

			// Stage 2 - save RawDataFile objects
			currentStage++;
			saveRawDataFiles(zipStream);
			if (status == TaskStatus.CANCELED)
				return;

			// Stage 3 - save PeakList objects
			currentStage++;
			savePeakLists(zipStream);
			if (status == TaskStatus.CANCELED)
				return;

			// Stage 4 - finish and close the temporary ZIP file
			currentStage++;
			currentSavedObjectName = null;
			zipStream.close();

			// Final check for cancel
			if (status == TaskStatus.CANCELED)
				return;

			// Move the temporary ZIP file to the final location
			if (saveFile.exists() && !saveFile.delete()) {
				throw new IOException("Could not delete old file " + saveFile);
			}
			boolean renameOK = tempFile.renameTo(saveFile);
			if (!renameOK) {
				throw new IOException("Could not move the temporary file "
						+ tempFile + " to the final location " + saveFile);
			}

			// Update the location of the project
			savedProject.setProjectFile(saveFile);

			logger.info("Finished saving the project to " + saveFile);

			status = TaskStatus.FINISHED;

		} catch (Throwable e) {

			status = TaskStatus.ERROR;

			if (currentSavedObjectName == null) {
				errorMessage = "Failed saving the project: "
						+ ExceptionUtils.exceptionToString(e);
			} else {
				errorMessage = "Failed saving the project. Error while saving "
						+ currentSavedObjectName + ": "
						+ ExceptionUtils.exceptionToString(e);
			}

		}
	}

	/**
	 * Save the version info
	 * 
	 * @throws java.io.IOException
	 */
	private void saveVersion(ZipOutputStream zipStream) throws IOException {

		zipStream.putNextEntry(new ZipEntry(VERSION_FILENAME));

		String MZmineVersion = MZmineCore.getMZmineVersion();

		zipStream.write(MZmineVersion.getBytes());

	}

	/**
	 * Save the configuration file.
	 * 
	 * @throws java.io.IOException
	 */
	private void saveConfiguration(ZipOutputStream zipStream)
			throws IOException {

		logger.info("Saving configuration file");

		currentSavedObjectName = "configuration";

		zipStream.putNextEntry(new ZipEntry(CONFIG_FILENAME));
		File tempConfigFile = File.createTempFile("mzmineconfig", ".tmp");

		MZmineCore.saveConfiguration(tempConfigFile);

		FileInputStream fileStream = new FileInputStream(tempConfigFile);

		StreamCopy copyMachine = new StreamCopy();
		copyMachine.copy(fileStream, zipStream);

		fileStream.close();
		tempConfigFile.delete();
	}

	/**
	 * Save the raw data files
	 * 
	 * @throws SAXException
	 * @throws TransformerConfigurationException
	 */
	private void saveRawDataFiles(ZipOutputStream zipStream)
			throws IOException, TransformerConfigurationException, SAXException {

		rawDataFileSaveHandler = new RawDataFileSaveHandler(zipStream);

		RawDataFile rawDataFiles[] = savedProject.getDataFiles();

		for (int i = 0; i < rawDataFiles.length; i++) {
			currentSavedObjectName = rawDataFiles[i].getName();
			rawDataFileSaveHandler.writeRawDataFile(rawDataFiles[i], i + 1);
			dataFilesIDMap.put(rawDataFiles[i], i + 1);
		}
	}

	/**
	 * Save the peak lists
	 * 
	 * @throws SAXException
	 * @throws TransformerConfigurationException
	 */
	private void savePeakLists(ZipOutputStream zipStream) throws IOException,
			TransformerConfigurationException, SAXException {

		peakListSaveHandler = new PeakListSaveHandler(zipStream);

		PeakList peakLists[] = savedProject.getPeakLists();

		for (int i = 0; i < peakLists.length; i++) {
			currentSavedObjectName = peakLists[i].getName();
			peakListSaveHandler.savePeakList(peakLists[i], i + 1,
					dataFilesIDMap);
		}
	}

	public Object[] getCreatedObjects() {
		throw new UnsupportedOperationException("Not supported.");
	}
}
