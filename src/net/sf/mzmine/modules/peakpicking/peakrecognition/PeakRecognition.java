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

package net.sf.mzmine.modules.peakpicking.peakrecognition;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.util.dialogs.ExitCode;

public class PeakRecognition implements BatchStep, ActionListener {

	private PeakRecognitionParameters parameters;

	private Desktop desktop;

	/**
	 * @see net.sf.mzmine.main.mzmineclient.MZmineModule#initModule(net.sf.mzmine.main.mzmineclient.MZmineCore)
	 */
	public void initModule() {

		this.desktop = MZmineCore.getDesktop();

		parameters = new PeakRecognitionParameters();
		desktop.addMenuItem(MZmineMenu.PEAKPICKING, "Peak recognition",
				"Resolving individual peaks within each chromatogram",
				KeyEvent.VK_P, true, this, null);
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		PeakList[] peakLists = desktop.getSelectedPeakLists();
		if (peakLists.length == 0) {
			desktop.displayErrorMessage("Please select at least one peak list");
			return;
		}

		for (int i = 0; i < peakLists.length; i++) {
			if (peakLists[i].getNumberOfRawDataFiles() > 1) {
				desktop
						.displayErrorMessage("Peak recognition can only be performed on peak lists which have a single column");
				return;
			}
		}

		ExitCode exitCode = setupParameters(parameters);
		if (exitCode != ExitCode.OK)
			return;

		runModule(null, peakLists, parameters.clone(), null);

	}

	/**
	 * @see net.sf.mzmine.modules.BatchStep#toString()
	 */
	public String toString() {
		return "Peak recognition";
	}

	/**
	 * @see net.sf.mzmine.modules.BatchStep#setupParameters(net.sf.mzmine.data.ParameterSet)
	 */
	public ExitCode setupParameters(ParameterSet parameters) {
		PeakRecognitionSetupDialog dialog = new PeakRecognitionSetupDialog(
				"Please set parameter values for " + toString(),
				(PeakRecognitionParameters) parameters);
		dialog.setVisible(true);
		return dialog.getExitCode();
	}

	/**
	 * @see net.sf.mzmine.main.mzmineclient.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	public void setParameters(ParameterSet parameters) {
		this.parameters = (PeakRecognitionParameters) parameters;
	}

	/**
	 * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.data.RawDataFile[],
	 *      net.sf.mzmine.data.AlignmentResult[],
	 *      net.sf.mzmine.data.ParameterSet,
	 *      net.sf.mzmine.taskcontrol.TaskGroupListener)
	 */
	public TaskGroup runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
			ParameterSet parameters, TaskGroupListener taskGroupListener) {
		// check peak lists
		if ((peakLists == null) || (peakLists.length == 0)) {
			desktop
					.displayErrorMessage("Please select peak lists for recognition");
			return null;
		}

		for (int i = 0; i < peakLists.length; i++) {
			if (peakLists[i].getNumberOfRawDataFiles() > 1) {
				desktop
						.displayErrorMessage("Peak recognition can only be performed on peak lists which have a single column");
				return null;
			}
		}

		// prepare a new group of tasks
		Task tasks[] = new PeakRecognitionTask[peakLists.length];
		for (int i = 0; i < peakLists.length; i++) {
			tasks[i] = new PeakRecognitionTask(peakLists[i],
					(PeakRecognitionParameters) parameters);
		}
		TaskGroup newGroup = new TaskGroup(tasks, null, taskGroupListener);

		// start the group
		newGroup.start();

		return newGroup;
	}

	public BatchStepCategory getBatchStepCategory() {
		return BatchStepCategory.PEAKPICKING;
	}

}
