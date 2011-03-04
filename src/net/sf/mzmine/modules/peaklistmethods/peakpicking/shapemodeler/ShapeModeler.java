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

package net.sf.mzmine.modules.peaklistmethods.peakpicking.shapemodeler;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListAppliedMethod;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.ExitCode;

public class ShapeModeler implements MZmineModule, ActionListener {

	final String helpID = GUIUtils.generateHelpID(this);

	public static final String MODULE_NAME = "Peak shape modeler (experimental)";

	private ShapeModelerParameters parameters;

	private Desktop desktop;

	public ShapeModeler() {

		this.desktop = MZmineCore.getDesktop();

		parameters = new ShapeModelerParameters();

		desktop.addMenuItem(MZmineMenu.PEAKLISTPICKING, MODULE_NAME,
				"Estimate peak shape according to selected peak shape model",
				0, true, this, null);

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

		// Verify previous applied methods
		boolean resolvedFlag = false;
		for (int i = 0; i < peakLists.length; i++) {
			for (PeakListAppliedMethod proc : peakLists[i].getAppliedMethods()) {

				if (proc.getDescription().contains("Peak deconvolution"))
					resolvedFlag = true;

				if (proc.getDescription().contains("normalization")) {
					desktop.displayErrorMessage("Peak list " + peakLists[i]
							+ " has been processed using \""
							+ proc.getDescription()
							+ "\", impossible to make peak shape modeling");
					return;
				}
			}

			if (!resolvedFlag)
				desktop.displayMessage(
						"Warning",
						"Peak list "
								+ peakLists[i]
								+ " has not been processed using \"Peak deconvolution\", result could be uncertain");
			resolvedFlag = false;
		}

		ExitCode exitCode = parameters.showSetupDialog();

		if (exitCode != ExitCode.OK)
			return;

		runModule(null, peakLists, parameters.clone());

	}

	/**
	 * @see net.sf.mzmine.modules.BatchStep#setupParameters(net.sf.mzmine.data.ParameterSet)
	 */

	/**
	 * @see net.sf.mzmine.modules.BatchStep#toString()
	 */
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
	 * @see 
	 *      net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.data.RawDataFile
	 *      [], net.sf.mzmine.data.AlignmentResult[],
	 *      net.sf.mzmine.data.ParameterSet,
	 *      net.sf.mzmine.taskcontrol.Task[]Listener)
	 */
	public Task[] runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
			ParameterSet parameters) {
		// check peak lists
		if ((peakLists == null) || (peakLists.length == 0)) {
			desktop.displayErrorMessage("Please select peak lists for recognition");
			return null;
		}

		// prepare a new group of tasks
		Task tasks[] = new ShapeModelerTask[peakLists.length];
		for (int i = 0; i < peakLists.length; i++) {
			tasks[i] = new ShapeModelerTask(peakLists[i],
					(ShapeModelerParameters) parameters);
		}

		MZmineCore.getTaskController().addTasks(tasks);

		return tasks;
	}

}
