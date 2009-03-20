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

package net.sf.mzmine.modules.peakpicking.msms;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

public class MsMsPeakPicker implements BatchStep, TaskListener,
		ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private Desktop desktop;
	private MsMsPeakPickerParameters parameters;

	/**
	 * @see net.sf.mzmine.main.mzmineclient.MZmineModule#initModule(net.sf.mzmine.main.mzmineclient.MZmineCore)
	 */
	public void initModule() {

		this.desktop = MZmineCore.getDesktop();
		parameters = new MsMsPeakPickerParameters();

		desktop.addMenuItem(MZmineMenu.PEAKPICKING, "MS/MS peaklist builder",
				"Building peaklist based on MS/MS results", KeyEvent.VK_M,
				false, this, null);

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

	/**
	 * @see net.sf.mzmine.modules.BatchStep#toString()
	 */
	public String toString() {
		return "MS/MS Peaklist builder";
	}

	/**
	 * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.data.RawDataFile[],
	 *      net.sf.mzmine.data.PeakList[], net.sf.mzmine.data.ParameterSet,
	 *      net.sf.mzmine.taskcontrol.TaskGroupListener)
	 */
	public TaskGroup runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
			ParameterSet parameters, TaskGroupListener taskGroupListener) {

		// check data files
		if ((dataFiles == null) || (dataFiles.length == 0)) {
			desktop
					.displayErrorMessage("Please select data files for peaklist building");
			return null;
		}

		// prepare a new task group
		Task tasks[] = new MsMsPeakPickingTask[dataFiles.length];
		for (int i = 0; i < dataFiles.length; i++) {
			tasks[i] = new MsMsPeakPickingTask(dataFiles[i],
					(MsMsPeakPickerParameters) parameters);
		}
		TaskGroup newGroup = new TaskGroup(tasks, this, taskGroupListener);

		// start this group
		newGroup.start();

		return newGroup;

	}

	/**
	 * @see net.sf.mzmine.main.mzmineclient.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	/**
	 * @see net.sf.mzmine.main.mzmineclient.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
	 */
	public void setParameters(ParameterSet parameters) {
		this.parameters = (MsMsPeakPickerParameters) parameters;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.TaskListener#taskStarted(net.sf.mzmine.taskcontrol.Task)
	 */
	public void taskStarted(Task task) {
		MsMsPeakPickingTask cropTask = (MsMsPeakPickingTask) task;
		logger.info("Running MS/MS Peaklist builder on "
				+ cropTask.getDataFile());
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
	 */
	public void taskFinished(Task task) {

		MsMsPeakPickingTask cropTask = (MsMsPeakPickingTask) task;

		if (task.getStatus() == Task.TaskStatus.FINISHED) {
			logger.info("Finished MS/MS Peaklist builder on "
					+ cropTask.getDataFile());
		}

		if (task.getStatus() == Task.TaskStatus.ERROR) {
			String msg = "Error while running MS/MS Peaklist builder on "
					+ cropTask.getDataFile() + ": " + task.getErrorMessage();
			logger.severe(msg);
			desktop.displayErrorMessage(msg);
		}

	}

	public BatchStepCategory getBatchStepCategory() {
		return BatchStepCategory.PEAKPICKING;
	}

	public ExitCode setupParameters(ParameterSet currentParameters) {
		ParameterSetupDialog dialog = new ParameterSetupDialog(
				"Please set parameter values for " + toString(),
				(SimpleParameterSet) currentParameters);

		dialog.setVisible(true);

		return dialog.getExitCode();
	}

}
