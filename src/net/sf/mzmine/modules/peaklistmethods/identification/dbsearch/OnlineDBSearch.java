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

package net.sf.mzmine.modules.peaklistmethods.identification.dbsearch;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * 
 */
public class OnlineDBSearch implements BatchStep, ActionListener {

	public static final String MODULE_NAME = "Online database search";

	private Desktop desktop;

	private OnlineDBSearchParameters parameters;

	private static OnlineDBSearch myInstance;

	/**
	 * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	/**
	 * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
	 */
	public void initModule() {
		this.desktop = MZmineCore.getDesktop();

		parameters = new OnlineDBSearchParameters();

		desktop.addMenuItem(MZmineMenu.IDENTIFICATION, MODULE_NAME,
				"Identification by searching online database", KeyEvent.VK_O,
				false, this, null);

		myInstance = this;

	}

	/**
	 * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
	 */
	public void setParameters(ParameterSet parameterValues) {
		this.parameters = (OnlineDBSearchParameters) parameterValues;
	}

	public BatchStepCategory getBatchStepCategory() {
		return BatchStepCategory.IDENTIFICATION;
	}

	public ExitCode setupParameters(ParameterSet parameters) {
		OnlineDBSearchDialog dialog = new OnlineDBSearchDialog(
				(OnlineDBSearchParameters) parameters, null);
		dialog.setVisible(true);
		return dialog.getExitCode();
	}

	public void actionPerformed(ActionEvent arg0) {

		PeakList[] selectedPeakLists = desktop.getSelectedPeakLists();

		if (selectedPeakLists.length < 1) {
			desktop.displayErrorMessage("Please select a peak list");
			return;
		}

		ExitCode exitCode = setupParameters(parameters);
		if (exitCode != ExitCode.OK)
			return;

		runModule(null, selectedPeakLists, parameters.clone());
	}

	public static void showSingleRowIdentificationDialog(PeakList peakList,
			PeakListRow row) {

		OnlineDBSearchParameters parameters = (OnlineDBSearchParameters) myInstance
				.getParameterSet();
		OnlineDBSearchDialog dialog = new OnlineDBSearchDialog(parameters, row);
		dialog.setVisible(true);

		ExitCode exitCode = dialog.getExitCode();
		if (exitCode != ExitCode.OK)
			return;

		// clone the parameters to avoid further changes
		parameters = (OnlineDBSearchParameters) parameters.clone();

		SingleRowIdentificationTask newTask = new SingleRowIdentificationTask(
				parameters, peakList, row);

		// execute the sequence
		MZmineCore.getTaskController().addTask(newTask);

	}

	/**
	 * @see 
	 *      net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.data.RawDataFile
	 *      [], net.sf.mzmine.data.PeakList[], net.sf.mzmine.data.ParameterSet,
	 *      net.sf.mzmine.taskcontrol.Task[]Listener)
	 */
	public Task[] runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
			ParameterSet parameters) {

		if (peakLists == null) {
			throw new IllegalArgumentException(
					"Cannot run identification without a peak list");
		}

		// prepare a new sequence of tasks
		Task tasks[] = new PeakListIdentificationTask[peakLists.length];
		for (int i = 0; i < peakLists.length; i++) {
			tasks[i] = new PeakListIdentificationTask(
					(OnlineDBSearchParameters) parameters, peakLists[i]);
		}

		// execute the sequence
		MZmineCore.getTaskController().addTasks(tasks);

		return tasks;

	}

	public String toString() {
		return MODULE_NAME;
	}

}
