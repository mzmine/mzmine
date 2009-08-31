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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.io.rawdataimport;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.modules.io.rawdataimport.fileformats.MzDataReadTask;
import net.sf.mzmine.modules.io.rawdataimport.fileformats.MzMLReadTask;
import net.sf.mzmine.modules.io.rawdataimport.fileformats.MzXMLReadTask;
import net.sf.mzmine.modules.io.rawdataimport.fileformats.NetCDFReadTask;
import net.sf.mzmine.modules.io.rawdataimport.fileformats.XcaliburRawFileReadTask;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * Raw data import module
 */
public class RawDataImporter implements MZmineModule, ActionListener, BatchStep {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private RawDataImporterParameters parameters;

	private Desktop desktop;

	/**
	 * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
	 */
	public void initModule() {

		this.desktop = MZmineCore.getDesktop();

		parameters = new RawDataImporterParameters();

		desktop.addMenuItem(MZmineMenu.RAWDATA, "Import raw data files",
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
		return "Raw data import";
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
		File file[] = rawDataImporterParameters.getFileNames();
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
				desktop
						.displayErrorMessage("Cannot determine file type of file "
								+ file[i]);
				logger.finest("Cannot determine file type of file " + file[i]);
				return null;
			}
		}

		MZmineCore.getTaskController().addTasks(openTasks);

		return openTasks;
	}

	public ExitCode setupParameters(ParameterSet parameterSet) {

		RawDataImporterParameters parameters = (RawDataImporterParameters) parameterSet;

		JFileChooser fileChooser = new JFileChooser();

		String path = (String) parameters
				.getParameterValue(RawDataImporterParameters.lastDirectory);
		if (path != null)
			fileChooser.setCurrentDirectory(new File(path));
		fileChooser.setMultiSelectionEnabled(true);

		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"NetCDF files", "cdf", "nc");
		fileChooser.addChoosableFileFilter(filter);

		filter = new FileNameExtensionFilter("mzDATA files", "mzDATA");
		fileChooser.addChoosableFileFilter(filter);

		filter = new FileNameExtensionFilter("mzML files", "mzML");
		fileChooser.addChoosableFileFilter(filter);

		filter = new FileNameExtensionFilter("XCalibur RAW files", "RAW");
		fileChooser.addChoosableFileFilter(filter);

		filter = new FileNameExtensionFilter("MZXML files", "mzxml");
		fileChooser.addChoosableFileFilter(filter);

		filter = new FileNameExtensionFilter("All raw data files", "cdf", "nc",
				"mzDATA", "mzML", "mzxml", "RAW");
		fileChooser.setFileFilter(filter);

		int returnVal = fileChooser.showOpenDialog(MZmineCore.getDesktop()
				.getMainFrame());

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File[] selectedFiles = fileChooser.getSelectedFiles();
			parameters.setFileNames(selectedFiles);
			parameters.setParameterValue(
					RawDataImporterParameters.lastDirectory, fileChooser
							.getCurrentDirectory().toString());

			return ExitCode.OK;
		} else
			return ExitCode.CANCEL;

	}

}
