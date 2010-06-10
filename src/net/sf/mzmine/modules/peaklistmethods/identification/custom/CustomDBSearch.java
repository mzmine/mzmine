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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.custom;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * 
 */
public class CustomDBSearch implements BatchStep, ActionListener {

	final String helpID = GUIUtils.generateHelpID(this);

	public static final String MODULE_NAME = "Custom database search";

	private Desktop desktop;

	private CustomDBSearchParameters parameters;

	/**
	 * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
	 */
	public void initModule() {

		this.desktop = MZmineCore.getDesktop();

		parameters = new CustomDBSearchParameters();

		desktop.addMenuItem(MZmineMenu.IDENTIFICATION, MODULE_NAME,
				"Identification by searching a custom database in CSV file",
				KeyEvent.VK_C, false, this, null);
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
	public void setParameters(ParameterSet parameterValues) {
		this.parameters = (CustomDBSearchParameters) parameterValues;
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

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
		Task tasks[] = new CustomDBSearchTask[peakLists.length];
		for (int i = 0; i < peakLists.length; i++) {
			tasks[i] = new CustomDBSearchTask(peakLists[i],
					(CustomDBSearchParameters) parameters);
		}

		// execute the sequence
		MZmineCore.getTaskController().addTasks(tasks);

		return tasks;

	}

	/**
	 * @see net.sf.mzmine.modules.BatchStep#setupParameters(net.sf.mzmine.data.ParameterSet)
	 */
	public ExitCode setupParameters(ParameterSet parameters) {
		// CustomDBSearchDialog dialog = new CustomDBSearchDialog(
		// (CustomDBSearchParameters) parameters);
		ParameterSetupDialog dialog = new ParameterSetupDialog(
				"Please set parameter values for " + toString(),
				(SimpleParameterSet) parameters, helpID);
		dialog.setVisible(true);
		return dialog.getExitCode();
	}

	public String toString() {
		return MODULE_NAME;
	}

	public BatchStepCategory getBatchStepCategory() {
		return BatchStepCategory.IDENTIFICATION;
	}

}
