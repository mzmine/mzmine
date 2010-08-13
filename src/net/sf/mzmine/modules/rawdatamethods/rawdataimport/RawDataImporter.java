/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.rawdataimport;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.AgilentCsvReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzDataReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzMLReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzXMLReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.NetCDFReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.XcaliburRawFileReadTask;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskEvent;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * Raw data import module
 */
public class RawDataImporter implements MZmineModule, ActionListener,
		BatchStep, TaskListener {

	final String helpID = GUIUtils.generateHelpID(this);

	public static final String MODULE_NAME = "Raw data import";

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private RawDataImporterParameters parameters;

	private Desktop desktop;

	/**
	 * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
	 */
	public void initModule() {

		this.desktop = MZmineCore.getDesktop();

		parameters = new RawDataImporterParameters();

		desktop.addMenuItem(MZmineMenu.RAWDATA, MODULE_NAME,
				"This module imports raw data files into the project",
				KeyEvent.VK_I, true, this, null);

	}

	/**
	 * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	/**
	 * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
	 */
	public void setParameters(ParameterSet parameters) {
		this.parameters = (RawDataImporterParameters) parameters;
	}

	/**
	 * @see net.sf.mzmine.modules.BatchStep#toString()
	 */
	public String toString() {
		return MODULE_NAME;
	}

	public void actionPerformed(ActionEvent event) {

		ExitCode setupExitCode = setupParameters(parameters);

		if (setupExitCode != ExitCode.OK) {
			return;
		}

		runModule(null, null, parameters);

	}

	public BatchStepCategory getBatchStepCategory() {
		return BatchStepCategory.PROJECT;
	}

	public Task[] runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
			ParameterSet parameters) {

		RawDataImporterParameters rawDataImporterParameters = (RawDataImporterParameters) parameters;
		String fileNames = (String) rawDataImporterParameters
				.getParameterValue(RawDataImporterParameters.fileNames);
		String splitFilenames[] = fileNames.split(":");

		Task openTasks[] = new Task[splitFilenames.length];

		for (int i = 0; i < splitFilenames.length; i++) {

			String filePath = splitFilenames[i];
			filePath = filePath.replaceAll("&colon", ":");
			filePath = filePath.replaceAll("&amp", "&");

			File file = new File(filePath);

			if ((!file.exists()) || (!file.canRead())) {
				desktop.displayErrorMessage("Cannot read file " + file);
				logger.warning("Cannot read file " + file);
				return null;
			}

			RawDataFileWriter newMZmineFile;
			try {
				newMZmineFile = MZmineCore.createNewFile(file.getName());
			} catch (IOException e) {
				desktop.displayErrorMessage("Could not create a new temporary file "
						+ e);
				logger.log(Level.SEVERE,
						"Could not create a new temporary file ", e);
				return null;
			}

			String extension = file.getName()
					.substring(file.getName().lastIndexOf(".") + 1)
					.toLowerCase();

			if (extension.endsWith("mzdata")) {
				openTasks[i] = new MzDataReadTask(file, newMZmineFile);
			}
			if (extension.endsWith("mzxml")) {
				openTasks[i] = new MzXMLReadTask(file, newMZmineFile);
			}
			if (extension.endsWith("mzml")) {
				openTasks[i] = new MzMLReadTask(file, newMZmineFile);
			}
			if (extension.endsWith("cdf")) {
				openTasks[i] = new NetCDFReadTask(file, newMZmineFile);
			}
			if (extension.endsWith("raw")) {
				openTasks[i] = new XcaliburRawFileReadTask(file, newMZmineFile);
			}
			if (extension.endsWith("xml")) {

				try {
					// Check the first 512 bytes of the file, to determine the
					// file type
					FileReader reader = new FileReader(file);
					char buffer[] = new char[512];
					reader.read(buffer);
					reader.close();
					String fileHeader = new String(buffer);
					if (fileHeader.contains("mzXML")) {
						openTasks[i] = new MzXMLReadTask(file, newMZmineFile);
					}
					if (fileHeader.contains("mzData")) {
						openTasks[i] = new MzDataReadTask(file, newMZmineFile);
					}
					if (fileHeader.contains("mzML")) {
						openTasks[i] = new MzMLReadTask(file, newMZmineFile);
					}
				} catch (Exception e) {
					// If an exception occurs, we just continue without
					// determining the file type
				}
			}

			if (extension.endsWith("csv")) {
				openTasks[i] = new AgilentCsvReadTask(file, newMZmineFile);
			}

			if (openTasks[i] == null) {
				desktop.displayErrorMessage("Cannot determine file type of file "
						+ file);
				logger.warning("Cannot determine file type of file " + file);
				return null;
			}

			openTasks[i].addTaskListener(this);

		}

		MZmineCore.getTaskController().addTasks(openTasks);

		return openTasks;
	}

	/**
	 * The statusChanged method of the TaskEvent interface
	 * 
	 * @param e
	 *            The TaskEvent which triggered this action
	 */
	public void statusChanged(TaskEvent e) {
		if (e.getStatus() == TaskStatus.FINISHED) {
			MZmineCore.getCurrentProject().addFile(
					(RawDataFile) e.getSource().getCreatedObjects()[0]);
		}

	}

	public ExitCode setupParameters(ParameterSet parameterSet) {

		RawDataImporterParameters parameters = (RawDataImporterParameters) parameterSet;

		String path = (String) parameters
				.getParameterValue(RawDataImporterParameters.lastDirectory);
		File lastPath = null;
		if (path != null)
			lastPath = new File(path);

		RawDataImporterDialog dialog = new RawDataImporterDialog(lastPath,
				helpID);
		dialog.setVisible(true);
		ExitCode exitCode = dialog.getExitCode();

		if (exitCode == ExitCode.OK) {

			String lastDir = dialog.getCurrentDirectory();
			parameters.setParameterValue(
					RawDataImporterParameters.lastDirectory, lastDir);

			File[] selectedFiles = dialog.getSelectedFiles();
			if (selectedFiles.length == 0)
				return ExitCode.CANCEL;

			StringBuilder fileNames = new StringBuilder();
			for (int i = 0; i < selectedFiles.length; i++) {
				String filePath = selectedFiles[i].getPath();
				filePath = filePath.replaceAll("&", "&amp");
				filePath = filePath.replaceAll(":", "&colon");
				if (i > 0)
					fileNames.append(":");
				fileNames.append(filePath);
			}

			parameters.setParameterValue(RawDataImporterParameters.fileNames,
					fileNames.toString());

		}

		return exitCode;

	}
}
