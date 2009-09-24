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

package net.sf.mzmine.modules.io.projectload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JInternalFrame;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.io.projectsave.ProjectSavingTask;
import net.sf.mzmine.project.ProjectManager;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.StreamCopy;

import org.dom4j.DocumentException;
import org.xml.sax.SAXException;

import de.schlichtherle.util.zip.ZipEntry;
import de.schlichtherle.util.zip.ZipFile;

public class ProjectOpeningTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;

	private File openFile;
	private MZmineProjectImpl newProject;

	private RawDataFileOpenHandler rawDataFileOpenHandler;
	private PeakListOpenHandler peakListOpenHandler;

	private int currentStage;
	private String currentLoadedObjectName;

	// This hashtable maps stored IDs to raw data file objects
	private Hashtable<Integer, RawDataFile> dataFilesIDMap;

	public ProjectOpeningTask(File openFile) {
		this.openFile = openFile;
		dataFilesIDMap = new Hashtable<Integer, RawDataFile>();
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		if (currentLoadedObjectName == null)
			return "Opening project " + openFile;
		return "Opening project " + openFile + " (" + currentLoadedObjectName
				+ ")";
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		switch (currentStage) {
		case 2:
			if (rawDataFileOpenHandler == null)
				return 0;
			return rawDataFileOpenHandler.getProgress();
		case 3:
			if (peakListOpenHandler == null)
				return 0;
			return peakListOpenHandler.getProgress();
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
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		try {

			logger.info("Started opening project " + openFile);
			status = TaskStatus.PROCESSING;

			// Create a new project
			newProject = new MZmineProjectImpl();
			newProject.setProjectFile(openFile);

			// Get project ZIP stream
			ZipFile zipFile = new ZipFile(openFile);

			// Stage 1 - check version and load configuration
			currentStage++;
			loadVersion(zipFile);
			loadConfiguration(zipFile);
			if (status == TaskStatus.CANCELED) {
				zipFile.close();
				return;
			}

			// Stage 2 - load raw data files
			currentStage++;
			loadRawDataFiles(zipFile);
			if (status == TaskStatus.CANCELED) {
				zipFile.close();
				return;
			}

			// Stage 3 - load peak lists
			currentStage++;
			loadPeakLists(zipFile);

			// Stage 4 - finish and close the project ZIP file
			currentStage++;
			zipFile.close();

			// Final check for cancel
			if (status == TaskStatus.CANCELED)
				return;

			// Close all open frames related to previous project
			JInternalFrame frames[] = MZmineCore.getDesktop()
					.getInternalFrames();
			for (JInternalFrame frame : frames) {
				// Use doDefailtCloseAction() instead of dispose() to protect
				// the TaskProgressWindow from disposing
				frame.doDefaultCloseAction();
			}

			// Replace the current project with the new one
			ProjectManager projectManager = MZmineCore.getProjectManager();
			projectManager.setCurrentProject(newProject);

			logger.info("Finished opening project " + openFile);

			status = TaskStatus.FINISHED;

		} catch (Throwable e) {

			// If project opening was canceled, parser was stopped by a
			// SAXException which can be safely ignored
			if (status == TaskStatus.CANCELED)
				return;

			status = TaskStatus.ERROR;
			errorMessage = "Failed opening project: "
					+ ExceptionUtils.exceptionToString(e);
		}
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#cancel()
	 */
	public void cancel() {

		logger.info("Canceling opening of project " + openFile);

		status = TaskStatus.CANCELED;

		if (rawDataFileOpenHandler != null)
			rawDataFileOpenHandler.cancel();

		if (peakListOpenHandler != null)
			peakListOpenHandler.cancel();

	}

	/**
	 * Load the version info from the ZIP file and checks whether such version
	 * can be opened with this MZmine
	 */
	private void loadVersion(ZipFile zipFile) throws IOException {

		logger.info("Checking project version");

		ZipEntry versionEntry = zipFile
				.getEntry(ProjectSavingTask.VERSION_FILENAME);

		if (versionEntry == null) {
			throw new IOException(
					"This file is not valid MZmine 2 project. It does not contain version information.");
		}

		InputStream versionInputStream = zipFile.getInputStream(versionEntry);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				versionInputStream));

		String mzmineVersion = MZmineCore.getMZmineVersion();
		String projectVersion = reader.readLine();
		reader.close();

		// Strip any extra characters from project version and convert it to
		// double
		Pattern p = Pattern.compile("(\\d+\\.\\d+)");
		Matcher m = p.matcher(projectVersion);
		m.find();
		projectVersion = m.group(1);

		double projectVersionNumber = Double.parseDouble(projectVersion);

		// Check if project was saved with compatible version
		if (projectVersionNumber < 1.96) {
			throw new IOException("This project was saved with MZmine version "
					+ projectVersion + " and cannot be opened in MZmine "
					+ mzmineVersion);
		}

	}

	/**
	 * Load the configuration file from the project zip file
	 */
	private void loadConfiguration(ZipFile zipFile) throws IOException {

		logger.info("Loading configuration file");

		ZipEntry configEntry = zipFile
				.getEntry(ProjectSavingTask.CONFIG_FILENAME);

		if (configEntry == null) {
			throw new IOException(
					"This file is not valid MZmine 2 project. It does not contain configuration data.");
		}

		InputStream configInputStream = zipFile.getInputStream(configEntry);
		File tempConfigFile = File.createTempFile("mzmineconfig", ".tmp");
		FileOutputStream fileStream = new FileOutputStream(tempConfigFile);
		StreamCopy copyMachine = new StreamCopy();
		copyMachine.copy(configInputStream, fileStream);
		fileStream.close();

		try {
			MZmineCore.loadConfiguration(tempConfigFile);
		} catch (DocumentException e) {
			throw (new IOException("Could not load configuration: "
					+ ExceptionUtils.exceptionToString(e)));
		}

		tempConfigFile.delete();
	}

	private void loadRawDataFiles(ZipFile zipFile) throws IOException,
			ParserConfigurationException, SAXException {

		logger.info("Loading raw data files");

		Pattern filePattern = Pattern
				.compile("Raw data file #([\\d]+) (.*)\\.xml");

		Enumeration zipEntries = zipFile.entries();
		while (zipEntries.hasMoreElements()) {

			// Canceled
			if (status == TaskStatus.CANCELED)
				return;

			ZipEntry entry = (ZipEntry) zipEntries.nextElement();
			String entryName = entry.getName();

			Matcher fileMatcher = filePattern.matcher(entryName);

			if (fileMatcher.matches()) {

				Integer fileID = Integer.parseInt(fileMatcher.group(1));
				currentLoadedObjectName = fileMatcher.group(2);

				String scansFileName = entryName.replaceFirst(".xml", ".scans");
				ZipEntry scansEntry = zipFile.getEntry(scansFileName);
				rawDataFileOpenHandler = new RawDataFileOpenHandler();
				RawDataFile newFile = rawDataFileOpenHandler.readRawDataFile(
						zipFile, scansEntry, entry);
				newProject.addFile(newFile);
				dataFilesIDMap.put(fileID, newFile);
			}

		}

	}

	private void loadPeakLists(ZipFile zipFile) throws IOException,
			ParserConfigurationException, SAXException {

		logger.info("Loading peak lists");

		Pattern filePattern = Pattern.compile("Peak list #([\\d]+) (.*)\\.xml");

		Enumeration zipEntries = zipFile.entries();
		while (zipEntries.hasMoreElements()) {

			// Canceled
			if (status == TaskStatus.CANCELED)
				return;

			ZipEntry entry = (ZipEntry) zipEntries.nextElement();
			String entryName = entry.getName();

			Matcher fileMatcher = filePattern.matcher(entryName);

			if (fileMatcher.matches()) {

				currentLoadedObjectName = fileMatcher.group(2);

				peakListOpenHandler = new PeakListOpenHandler();
				PeakList newPeakList = peakListOpenHandler.readPeakList(
						zipFile, entry, dataFilesIDMap);
				newProject.addPeakList(newPeakList);
			}

		}

	}

	public Object[] getCreatedObjects() {
		ArrayList<Object> newObjects = new ArrayList<Object>();
		for (RawDataFile file : newProject.getDataFiles()) {
			newObjects.add(file);
		}
		for (PeakList peakList : newProject.getPeakLists()) {
			newObjects.add(peakList);
		}
		return newObjects.toArray();
	}
}
