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

package net.sf.mzmine.modules.peaklistmethods.normalization.rtnormalizer;

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
public class RTNormalizer implements BatchStep, ActionListener {

	final String helpID = GUIUtils.generateHelpID(this);

	public static final String MODULE_NAME = "Retention time normalizer";

	private RTNormalizerParameters parameters;

	private Desktop desktop;

	public RTNormalizer() {

		this.desktop = MZmineCore.getDesktop();

		parameters = new RTNormalizerParameters();

		desktop.addMenuItem(
				MZmineMenu.NORMALIZATION,
				MODULE_NAME,
				"Retention time normalization using common, high intensity peaks",
				KeyEvent.VK_R, false, this, null);

	}

	public String toString() {
		return MODULE_NAME;
	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		PeakList[] selectedPeakLists = desktop.getSelectedPeakLists();

		// check peak lists
		if ((selectedPeakLists == null) || (selectedPeakLists.length < 2)) {
			desktop.displayErrorMessage("Please select at least 2 peak lists for normalization");
			return;
		}

		ExitCode exitCode = parameters.showSetupDialog();

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

		// check peak lists
		if ((peakLists == null) || (peakLists.length < 2)) {
			desktop.displayErrorMessage("Please select at least 2 peak lists for normalization");
			return null;
		}

		// prepare a new group of tasks
		RTNormalizerTask task = new RTNormalizerTask(peakLists,
				 parameters);

		MZmineCore.getTaskController().addTask(task);

		return new Task[] { task };

	}

	public BatchStepCategory getBatchStepCategory() {
		return BatchStepCategory.NORMALIZATION;
	}

}
