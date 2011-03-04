/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.RawDataFileWriter;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.AgilentCsvReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzDataReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzMLReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.MzXMLReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.NetCDFReadTask;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats.XcaliburRawFileReadTask;
import net.sf.mzmine.parameters.ParameterSet;
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

	/**
	 */
	public RawDataImporter() {

		parameters = new RawDataImporterParameters();

		MZmineCore.getDesktop().addMenuItem(MZmineMenu.RAWDATA, MODULE_NAME,
				"This module imports raw data files into the project",
				KeyEvent.VK_I, true, this, null);

	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	/**
	 * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
	 */

	/**
	 * @see net.sf.mzmine.modules.BatchStep#toString()
	 */
	public String toString() {
		return MODULE_NAME;
	}

	public void actionPerformed(ActionEvent event) {

		ExitCode setupExitCode = parameters.showSetupDialog();

		if (setupExitCode != ExitCode.OK) {
			return;
		}

		runModule(null, null, parameters.clone());

	}

	public BatchStepCategory getBatchStepCategory() {
		return BatchStepCategory.PROJECT;
	}

	public Task[] runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
			ParameterSet parameters) {

		File fileNames[] = parameters.getParameter(
				RawDataImporterParameters.fileNames).getValue();

		Task openTasks[] = new Task[fileNames.length];

		for (int i = 0; i < fileNames.length; i++) {

			if ((!fileNames[i].exists()) || (!fileNames[i].canRead())) {
				MZmineCore.getDesktop().displayErrorMessage(
						"Cannot read file " + fileNames[i]);
				logger.warning("Cannot read file " + fileNames[i]);
				return null;
			}

			RawDataFileWriter newMZmineFile;
			try {
				newMZmineFile = MZmineCore
						.createNewFile(fileNames[i].getName());
			} catch (IOException e) {
				MZmineCore.getDesktop().displayErrorMessage(
						"Could not create a new temporary file " + e);
				logger.log(Level.SEVERE,
						"Could not create a new temporary file ", e);
				return null;
			}

			String extension = fileNames[i].getName()
					.substring(fileNames[i].getName().lastIndexOf(".") + 1)
					.toLowerCase();

			if (extension.endsWith("mzdata")) {
				openTasks[i] = new MzDataReadTask(fileNames[i], newMZmineFile);
			}
			if (extension.endsWith("mzxml")) {
				openTasks[i] = new MzXMLReadTask(fileNames[i], newMZmineFile);
			}
			if (extension.endsWith("mzml")) {
				openTasks[i] = new MzMLReadTask(fileNames[i], newMZmineFile);
			}
			if (extension.endsWith("cdf")) {
				openTasks[i] = new NetCDFReadTask(fileNames[i], newMZmineFile);
			}
			if (extension.endsWith("raw")) {
				openTasks[i] = new XcaliburRawFileReadTask(fileNames[i],
						newMZmineFile);
			}
			if (extension.endsWith("xml")) {

				try {
					// Check the first 512 bytes of the file, to determine the
					// file type
					FileReader reader = new FileReader(fileNames[i]);
					char buffer[] = new char[512];
					reader.read(buffer);
					reader.close();
					String fileHeader = new String(buffer);
					if (fileHeader.contains("mzXML")) {
						openTasks[i] = new MzXMLReadTask(fileNames[i],
								newMZmineFile);
					}
					if (fileHeader.contains("mzData")) {
						openTasks[i] = new MzDataReadTask(fileNames[i],
								newMZmineFile);
					}
					if (fileHeader.contains("mzML")) {
						openTasks[i] = new MzMLReadTask(fileNames[i],
								newMZmineFile);
					}
				} catch (Exception e) {
					// If an exception occurs, we just continue without
					// determining the file type
				}
			}

			if (extension.endsWith("csv")) {
				openTasks[i] = new AgilentCsvReadTask(fileNames[i],
						newMZmineFile);
			}

			if (openTasks[i] == null) {
				MZmineCore.getDesktop().displayErrorMessage(
						"Cannot determine file type of file " + fileNames[i]);
				logger.warning("Cannot determine file type of file "
						+ fileNames[i]);
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

}
