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

package net.sf.mzmine.modules.visualization.intensityplot;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * Peak intensity plot module
 */
public class IntensityPlotModule implements MZmineProcessingModule {

	private IntensityPlotParameters parameters = new IntensityPlotParameters();

	private static IntensityPlotModule myInstance;

	public IntensityPlotModule() {
		myInstance = this;
	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#toString()
	 */
	public String toString() {
		return "Peak intensity plot";
	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	public static void showIntensityPlot(PeakList peakList, PeakListRow rows[]) {

		myInstance.parameters.getParameter(IntensityPlotParameters.peakList)
				.setValue(new PeakList[] { peakList });

		myInstance.parameters.getParameter(IntensityPlotParameters.dataFiles)
				.setChoices(peakList.getRawDataFiles());

		myInstance.parameters.getParameter(IntensityPlotParameters.dataFiles)
				.setValue(peakList.getRawDataFiles());

		myInstance.parameters
				.getParameter(IntensityPlotParameters.selectedRows).setChoices(
						rows);
		myInstance.parameters
				.getParameter(IntensityPlotParameters.selectedRows).setValue(
						rows);

		Parameter projectParams[] = MZmineCore.getCurrentProject()
				.getParameters();
		Object xAxisSources[] = new Object[projectParams.length + 1];
		xAxisSources[0] = IntensityPlotParameters.rawDataFilesOption;
		System.arraycopy(projectParams, 0, xAxisSources, 1,
				projectParams.length);
		myInstance.parameters.getParameter(
				IntensityPlotParameters.xAxisValueSource).setChoices(
				xAxisSources);

		ExitCode exitCode = myInstance.parameters.showSetupDialog();

		if (exitCode == ExitCode.OK)
			myInstance.runModule(myInstance.parameters.clone());

	}

	@Override
	public Task[] runModule(ParameterSet parameters) {

		IntensityPlotFrame newFrame = new IntensityPlotFrame(parameters);
		MZmineCore.getDesktop().addInternalFrame(newFrame);
		return null;
	}

	@Override
	public MZmineModuleCategory getModuleCategory() {
		return MZmineModuleCategory.VISUALIZATIONPEAKLIST;
	}

}