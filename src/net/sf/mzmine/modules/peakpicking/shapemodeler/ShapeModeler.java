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

package net.sf.mzmine.modules.peakpicking.shapemodeler;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListAppliedMethod;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.main.mzmineclient.MZmineModule;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.dialogs.ExitCode;

public class ShapeModeler implements MZmineModule, ActionListener {

	private ShapeModelerParameters parameters;

	private Desktop desktop;
	
	/**
     * @see net.sf.mzmine.main.mzmineclient.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {
    	
		this.desktop = MZmineCore.getDesktop();

		parameters = new ShapeModelerParameters();
		
		desktop.addMenuItem(MZmineMenu.PEAKPICKING, "Peak shape modeler",
				"Calculate peak shape accroding to selected peak shape model",
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
			for (PeakListAppliedMethod proc: peakLists[i].getAppliedMethods()){
				
				if (proc.getDescription().contains("Peak recognition"))
					resolvedFlag = true;

				if (proc.getDescription().contains("normalization")){
					desktop.displayErrorMessage("Peak list " + peakLists[i] + 
					" has been processed using \"" + proc.getDescription() +"\", impossible to make peak shape modeling");
					return;
				}
			}

			if (!resolvedFlag)
				desktop.displayMessage("Warning", "Peak list " + peakLists[i] + 
						" has not been processed using \"Peak recognition\", result could be uncertain");
			resolvedFlag = false;
		}

		ExitCode exitCode = setupParameters(parameters);
		if (exitCode != ExitCode.OK)
			return;

		runModule(null, peakLists, parameters.clone());

	}
	
	/**
	 * @see net.sf.mzmine.modules.BatchStep#setupParameters(net.sf.mzmine.data.ParameterSet)
	 */
	public ExitCode setupParameters(ParameterSet parameters) {
		ShapeModelerSetupDialog dialog = new ShapeModelerSetupDialog(
				"Please set parameter values for " + toString(),
				(ShapeModelerParameters) parameters);
		dialog.setVisible(true);
		return dialog.getExitCode();
	}


    /**
     * @see net.sf.mzmine.modules.BatchStep#toString()
     */
    public String toString() {
        return "Peak shape modeler";
    }

	/**
	 * @see net.sf.mzmine.main.mzmineclient.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	public void setParameters(ParameterSet parameters) {
		this.parameters = (ShapeModelerParameters) parameters;
	}

	/**
	 * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.data.RawDataFile[],
	 *      net.sf.mzmine.data.AlignmentResult[],
	 *      net.sf.mzmine.data.ParameterSet,
	 *      net.sf.mzmine.taskcontrol.Task[]Listener)
	 */
	public Task[] runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
			ParameterSet parameters) {
		// check peak lists
		if ((peakLists == null) || (peakLists.length == 0)) {
			desktop
					.displayErrorMessage("Please select peak lists for recognition");
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
