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

package net.sf.mzmine.modules.visualization.twod;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * 2D visualizer using JFreeChart library
 */
public class TwoDVisualizerModule implements MZmineProcessingModule {

	private static TwoDVisualizerModule myInstance;

	private TwoDParameters parameters = new TwoDParameters();

	public TwoDVisualizerModule() {
		myInstance = this;
	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#toString()
	 */
	public String toString() {
		return "2D visualizer";
	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	public static void show2DVisualizerSetupDialog(RawDataFile dataFile) {
		show2DVisualizerSetupDialog(dataFile, null, null);
	}

	public static void show2DVisualizerSetupDialog(RawDataFile dataFile,
			Range mzRange, Range rtRange) {

		myInstance.parameters.getParameter(TwoDParameters.dataFiles).setValue(
				new RawDataFile[] { dataFile });

		if (rtRange != null)
			myInstance.parameters.getParameter(
					TwoDParameters.retentionTimeRange).setValue(rtRange);
		if (mzRange != null)
			myInstance.parameters.getParameter(TwoDParameters.mzRange)
					.setValue(mzRange);

		ExitCode exitCode = myInstance.parameters.showSetupDialog();

		if (exitCode != ExitCode.OK)
			return;

		int msLevel = myInstance.parameters
				.getParameter(TwoDParameters.msLevel).getValue();
		rtRange = myInstance.parameters.getParameter(
				TwoDParameters.retentionTimeRange).getValue();
		mzRange = myInstance.parameters.getParameter(TwoDParameters.mzRange)
				.getValue();

		TwoDVisualizerWindow newWindow = new TwoDVisualizerWindow(dataFile,
				msLevel, rtRange, mzRange, myInstance.parameters);

		MZmineCore.getDesktop().addInternalFrame(newWindow);

	}

	@Override
	public Task[] runModule(ParameterSet parameters) {

		RawDataFile dataFiles[] = myInstance.parameters.getParameter(
				TwoDParameters.dataFiles).getValue();

		if ((dataFiles == null) || (dataFiles.length == 0)) {
			MZmineCore.getDesktop().displayErrorMessage(
					"Please select raw data file");
			return null;
		}
		
		int msLevel = myInstance.parameters
				.getParameter(TwoDParameters.msLevel).getValue();
		Range rtRange = myInstance.parameters.getParameter(
				TwoDParameters.retentionTimeRange).getValue();
		Range mzRange = myInstance.parameters.getParameter(
				TwoDParameters.mzRange).getValue();
		TwoDVisualizerWindow newWindow = new TwoDVisualizerWindow(dataFiles[0],
				msLevel, rtRange, mzRange, myInstance.parameters);

		MZmineCore.getDesktop().addInternalFrame(newWindow);

		return null;

	}

	@Override
	public MZmineModuleCategory getModuleCategory() {
		return MZmineModuleCategory.VISUALIZATIONRAWDATA;
	}

}