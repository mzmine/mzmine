/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.anothercentroid;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStepPeakPicking;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;

/**
 * 
 * 
 */
public class AnotherCentroidPicker implements BatchStepPeakPicking,
		TaskListener, ActionListener {

	private AnotherCentroidPickerParameters parameters;

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private Desktop desktop;

	/**
	 * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
	 */
	public void initModule() {

		this.desktop = MZmineCore.getDesktop();

		parameters = new AnotherCentroidPickerParameters();

		desktop.addMenuItem(MZmineMenu.PEAKPICKING, "Another centroid peak detector",
				this, null, KeyEvent.VK_C, false, true);

	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		RawDataFile[] dataFiles = desktop.getSelectedDataFiles();
		if (dataFiles.length == 0) {
			desktop.displayErrorMessage("Please select at least one data file");
			return;
		}

		ExitCode exitCode = setupParameters(parameters);
		if (exitCode != ExitCode.OK)
			return;

		runModule(dataFiles, null, parameters.clone(), null);

	}

	public void taskStarted(Task task) {
		AnotherCentroidPickerTask centTask = (AnotherCentroidPickerTask) task;
		logger
				.info("Running centroid peak picker on "
						+ centTask.getDataFile());
	}

	public void taskFinished(Task task) {

		AnotherCentroidPickerTask centTask = (AnotherCentroidPickerTask) task;

		if (task.getStatus() == Task.TaskStatus.FINISHED) {

			logger.info("Finished centroid peak picker on "
					+ centTask.getDataFile());

		}

		if (task.getStatus() == Task.TaskStatus.ERROR) {
			String msg = "Error while running centroid peak picker on file "
					+ centTask.getDataFile() + ": " + task.getErrorMessage();
			logger.severe(msg);
			desktop.displayErrorMessage(msg);
		}

	}

	/**
	 * @see net.sf.mzmine.modules.BatchStep#toString()
	 */
	public String toString() {
		return "Centroid peak detector";
	}

	/**
	 * @see net.sf.mzmine.modules.BatchStep#setupParameters(net.sf.mzmine.data.ParameterSet)
	 */
	public ExitCode setupParameters(ParameterSet currentParameters) {
		ParameterSetupDialog dialog = new ParameterSetupDialog(
				"Please set parameter values for " + toString(),
				(SimpleParameterSet) currentParameters);
		dialog.setVisible(true);
		return dialog.getExitCode();
	}

	/**
	 * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	public void setParameters(ParameterSet parameters) {
		this.parameters = (AnotherCentroidPickerParameters) parameters;
	}

	/**
	 * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.io.RawDataFile[],
	 *      net.sf.mzmine.data.AlignmentResult[],
	 *      net.sf.mzmine.data.ParameterSet,
	 *      net.sf.mzmine.taskcontrol.TaskGroupListener)
	 */
	public TaskGroup runModule(RawDataFile[] dataFiles,
			PeakList[] alignmentResults, ParameterSet parameters,
			TaskGroupListener taskGroupListener) {

		// check data files
		if ((dataFiles == null) || (dataFiles.length == 0)) {
			desktop
					.displayErrorMessage("Please select data files for peak picking");
			return null;
		}

		// prepare a new group of tasks
		Task tasks[] = new AnotherCentroidPickerTask[dataFiles.length];
		for (int i = 0; i < dataFiles.length; i++) {
			tasks[i] = new AnotherCentroidPickerTask(dataFiles[i],
					(AnotherCentroidPickerParameters) parameters);
		}
		TaskGroup newGroup = new TaskGroup(tasks, this, taskGroupListener);

		// start the group
		newGroup.start();

		return newGroup;

	}

}
