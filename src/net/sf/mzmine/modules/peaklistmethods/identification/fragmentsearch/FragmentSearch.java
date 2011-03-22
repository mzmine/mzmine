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
package net.sf.mzmine.modules.peaklistmethods.identification.fragmentsearch;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * 
 */
public class FragmentSearch implements BatchStep, ActionListener {

	final String helpID = GUIUtils.generateHelpID(this);

	public static final String MODULE_NAME = "Fragment search";
	private Desktop desktop;
	private FragmentSearchParameters parameters;

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	public FragmentSearch() {
		this.desktop = MZmineCore.getDesktop();

		parameters = new FragmentSearchParameters();

		desktop.addMenuItem(MZmineMenu.IDENTIFICATION, MODULE_NAME,
				"Identification of in-source fragmentation peaks",
				KeyEvent.VK_F, false, this, null);

	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
	 */
	public void setParameters(ParameterSet parameterValues) {
		this.parameters = (FragmentSearchParameters) parameterValues;
	}

	/**
	 * @see net.sf.mzmine.modules.batchmode.BatchStep#getBatchStepCategory()
	 */
	public BatchStepCategory getBatchStepCategory() {
		return BatchStepCategory.IDENTIFICATION;
	}

	/**
	 * @see net.sf.mzmine.modules.batchmode.BatchStep#setupParameters(net.sf.mzmine.data.ParameterSet)
	 */

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		PeakList[] peakLists = desktop.getSelectedPeakLists();

		if (peakLists.length == 0) {
			desktop.displayErrorMessage("Please select a peak lists to process");
			return;
		}

		ExitCode exitCode = parameters.showSetupDialog();
		if (exitCode != ExitCode.OK)
			return;

		runModule(null, peakLists, parameters.clone());

	}

	/**
	 * @see 
	 *      net.sf.mzmine.modules.batchmode.BatchStep#runModule(net.sf.mzmine.data
	 *      .RawDataFile[], net.sf.mzmine.data.PeakList[],
	 *      net.sf.mzmine.data.ParameterSet,
	 *      net.sf.mzmine.taskcontrol.Task[]Listener)
	 */
	public Task[] runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
			ParameterSet parameters) {
		if (peakLists == null) {
			throw new IllegalArgumentException(
					"Cannot run identification without a peak list");
		}

		// prepare a new sequence of tasks
		Task tasks[] = new FragmentSearchTask[peakLists.length];
		for (int i = 0; i < peakLists.length; i++) {
			tasks[i] = new FragmentSearchTask(parameters, peakLists[i]);
		}

		// execute the sequence
		MZmineCore.getTaskController().addTasks(tasks);

		return tasks;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return MODULE_NAME;
	}

}
