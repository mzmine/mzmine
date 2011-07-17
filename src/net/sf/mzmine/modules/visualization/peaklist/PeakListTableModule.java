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

package net.sf.mzmine.modules.visualization.peaklist;

import java.util.logging.Logger;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;

public class PeakListTableModule implements MZmineProcessingModule {

	private ParameterSet parameters = new PeakListTableParameters();

	private static PeakListTableModule myInstance;

	private Logger logger = Logger.getLogger(this.getClass().getName());

	public PeakListTableModule() {
		myInstance = this;
	}

	public static PeakListTableModule getInstance() {
		return myInstance;
	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#toString()
	 */
	public String toString() {
		return "Peak list table visualizer";
	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	public static void showNewPeakListVisualizerWindow(PeakList peakList) {
		ParameterSet parametersCopy = myInstance.getParameterSet().clone();
		PeakListTableWindow window = new PeakListTableWindow(peakList,
				parametersCopy);
		MZmineCore.getDesktop().addInternalFrame(window);
	}

	@Override
	public Task[] runModule(ParameterSet parameters) {

		PeakList peakLists[] = parameters.getParameter(
				PeakListTableParameters.peakLists).getValue();

		if ((peakLists == null) || (peakLists.length == 0)) {
			MZmineCore.getDesktop().displayErrorMessage(
					"Please select a peak list");
			return null;
		}

		for (PeakList peakList : peakLists) {

			logger.finest("Showing a new peak list table view");

			PeakListTableWindow alignmentResultView = new PeakListTableWindow(
					peakList, parameters.clone());

			MZmineCore.getDesktop().addInternalFrame(alignmentResultView);

		}
		return null;
	}

	@Override
	public MZmineModuleCategory getModuleCategory() {
		return MZmineModuleCategory.VISUALIZATIONPEAKLIST;
	}

}