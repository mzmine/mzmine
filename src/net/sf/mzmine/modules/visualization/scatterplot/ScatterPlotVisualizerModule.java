/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.scatterplot;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;

public class ScatterPlotVisualizerModule implements MZmineProcessingModule {

	private ScatterPlotParameters parameters = new ScatterPlotParameters();

	public static void showNewScatterPlotWindow(PeakList peakList) {

		if (peakList.getNumberOfRawDataFiles() < 2) {
			MZmineCore
					.getDesktop()
					.displayErrorMessage(
							"There is only one raw data file in the selected "
									+ "peak list, it is necessary at least two for comparison");
			return;
		}

		ScatterPlotWindow newWindow = new ScatterPlotWindow(peakList);

		MZmineCore.getDesktop().addInternalFrame(newWindow);

	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#toString()
	 */
	public String toString() {
		return "Scatter plot";
	}

	public ParameterSet getParameterSet() {
		return parameters;
	}

	@Override
	public Task[] runModule(ParameterSet parameters) {

		PeakList peakLists[] = parameters.getParameter(
				ScatterPlotParameters.peakLists).getValue();
		
		if ((peakLists == null) || (peakLists.length == 0)) {
			MZmineCore.getDesktop().displayErrorMessage(
					"Please select a peak list");
			return null;
		}

		PeakList peakList = peakLists[0];
		if (peakList.getNumberOfRawDataFiles() < 2) {
			MZmineCore
					.getDesktop()
					.displayErrorMessage(
							"There is only one raw data file in the selected "
									+ "peak list, it is necessary at least two for comparison");
			return null;
		}

		ScatterPlotWindow newWindow = new ScatterPlotWindow(peakList);
		MZmineCore.getDesktop().addInternalFrame(newWindow);

		return null;
	}

	@Override
	public MZmineModuleCategory getModuleCategory() {
		return MZmineModuleCategory.VISUALIZATIONPEAKLIST;
	}

}