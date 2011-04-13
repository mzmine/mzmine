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
package net.sf.mzmine.modules.peaklistmethods.alignment.graph;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * 
 */
public class GraphAligner implements BatchStep, ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private GraphAlignerParameters parameters;
	private Desktop desktop;

	public GraphAligner() {

		this.desktop = MZmineCore.getDesktop();

		parameters = new GraphAlignerParameters();

		desktop.addMenuItem(MZmineMenu.ALIGNMENT, "Graph alignment",
				"Alignment of two or more data sets using dynamic programming.", KeyEvent.VK_G, false, this, null);
		
	}

        @Override
	public String toString() {
		return "Graph alignment";
	}

	
	public ParameterSet getParameterSet() {
		return parameters;
	}
	

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		PeakList[] peakLists = desktop.getSelectedPeakLists();

		if (peakLists.length == 0) {
			desktop.displayErrorMessage("Please select peak lists for alignment");
			return;
		}

		// Setup parameters
		ExitCode exitCode = parameters.showSetupDialog();
		if (exitCode != ExitCode.OK) {
			return;
		}

		runModule(null, peakLists, parameters.clone());

	}	

	public void taskStarted(Task task) {
		logger.info("Running Graph alignment");
	}

	public void taskFinished(Task task) {
		if (task.getStatus() == TaskStatus.FINISHED) {
			logger.info("Finished alignment on " + ((GraphAlignerTask) task).getTaskDescription());
		}

		if (task.getStatus() == TaskStatus.ERROR) {

			String msg = "Error while alignment on .. " + ((GraphAlignerTask) task).getErrorMessage();
			logger.severe(msg);
			desktop.displayErrorMessage(msg);

		}
	}

        public Task[] runModule(RawDataFile[] dataFiles, PeakList[] peakLists, ParameterSet parameters) {
                // check peak lists
		if ((peakLists == null) || (peakLists.length == 0)) {
			desktop.displayErrorMessage("Please select peak lists for alignment");
			return null;
		}

		// prepare a new group with just one task
		Task task = new GraphAlignerTask(peakLists, parameters);

		MZmineCore.getTaskController().addTask(task);

		return new Task[]{task};
        }

        public BatchStepCategory getBatchStepCategory() {
                return BatchStepCategory.ALIGNMENT;
        }


}
