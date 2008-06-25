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

package net.sf.mzmine.modules.peakpicking.threestep;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.util.dialogs.ExitCode;

public class ThreeStepPicker implements BatchStep, TaskListener, ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private ThreeStepPickerParameters parameters;

	private Desktop desktop;

	/**
	 * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
	 */
	public void initModule() {

		this.desktop = MZmineCore.getDesktop();

		parameters = new ThreeStepPickerParameters();
		desktop.addMenuItem(MZmineMenu.PEAKPICKING, "Two steps peak detector",
				"TODO write description", KeyEvent.VK_T, this, null);
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
		ThreeStepPickerTask rtTask = (ThreeStepPickerTask) task;
		logger.info("Running two step peak picker on " + rtTask.getDataFile());

	}

	public void taskFinished(Task task) {

		ThreeStepPickerTask rtTask = (ThreeStepPickerTask) task;

		if (task.getStatus() == Task.TaskStatus.FINISHED) {
			logger.info("Finished two steps peak picker on "
					+ rtTask.getDataFile());
		}

		if (task.getStatus() == Task.TaskStatus.ERROR) {
			String msg = "Error while running two steps peak picker on file "
					+ rtTask.getDataFile() + ": " + task.getErrorMessage();
			logger.severe(msg);
			desktop.displayErrorMessage(msg);
		}

	}

	/**
	 * @see net.sf.mzmine.modules.BatchStep#toString()
	 */
	public String toString() {
		return "Two steps peak detector ";
	}

	/**
	 * @see net.sf.mzmine.modules.BatchStep#setupParameters(net.sf.mzmine.data.ParameterSet)
	 */
	public ExitCode setupParameters(ParameterSet parameters) {
		ThreeStepPickerSetupDialog dialog = new ThreeStepPickerSetupDialog(
				"Please set parameter values for " + toString(),
				(ThreeStepPickerParameters) parameters);
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
		this.parameters = (ThreeStepPickerParameters) parameters;
	}

	/**
	 * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.data.RawDataFile[],
	 *      net.sf.mzmine.data.AlignmentResult[],
	 *      net.sf.mzmine.data.ParameterSet,
	 *      net.sf.mzmine.taskcontrol.TaskGroupListener)
	 */
	public TaskGroup runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
			ParameterSet parameters, TaskGroupListener taskGroupListener) {
		// check data files
		if ((dataFiles == null) || (dataFiles.length == 0)) {
			desktop
					.displayErrorMessage("Please select data files for peak picking");
			return null;
		}

		// prepare a new group of tasks
		Task tasks[] = new ThreeStepPickerTask[dataFiles.length];
		for (int i = 0; i < dataFiles.length; i++) {
			tasks[i] = new ThreeStepPickerTask(dataFiles[i],
					(ThreeStepPickerParameters) parameters);
		}
		TaskGroup newGroup = new TaskGroup(tasks, this, taskGroupListener);

		// start the group
		newGroup.start();

		return newGroup;
	}

	public BatchStepCategory getBatchStepCategory() {
		return BatchStepCategory.PEAKPICKING;
	}


}
