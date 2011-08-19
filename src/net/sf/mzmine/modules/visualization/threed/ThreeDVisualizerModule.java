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

package net.sf.mzmine.modules.visualization.threed;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * 3D visualizer module
 */
public class ThreeDVisualizerModule implements MZmineProcessingModule {

	private static ThreeDVisualizerModule myInstance;

	private ThreeDVisualizerParameters parameters = new ThreeDVisualizerParameters();

	public ThreeDVisualizerModule() {
		myInstance = this;
	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#toString()
	 */
	public String toString() {
		return "3D visualizer";
	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
	 */

	public static void setupNew3DVisualizer(RawDataFile dataFile) {
		setupNew3DVisualizer(dataFile, null, null);
	}

	public static void setupNew3DVisualizer(RawDataFile dataFile,
			Range mzRange, Range rtRange) {

		myInstance.parameters
				.getParameter(ThreeDVisualizerParameters.dataFiles).setValue(
						new RawDataFile[] { dataFile });
		myInstance.parameters.getParameter(
				ThreeDVisualizerParameters.retentionTimeRange)
				.setValue(rtRange);
		myInstance.parameters.getParameter(ThreeDVisualizerParameters.mzRange)
				.setValue(mzRange);

		ExitCode exitCode = myInstance.parameters.showSetupDialog();

		if (exitCode == ExitCode.OK)
			myInstance.runModule(myInstance.parameters.clone());

	}

	@Override
	public Task[] runModule(ParameterSet parameters) {
		RawDataFile dataFiles[] = myInstance.parameters.getParameter(
				ThreeDVisualizerParameters.dataFiles).getValue();

		if ((dataFiles == null) || (dataFiles.length == 0)) {
			MZmineCore.getDesktop().displayErrorMessage(
					"Please select raw data file");
			return null;
		}

		int msLevel = myInstance.parameters.getParameter(
				ThreeDVisualizerParameters.msLevel).getValue();
		Range rtRange = myInstance.parameters.getParameter(
				ThreeDVisualizerParameters.retentionTimeRange).getValue();
		Range mzRange = myInstance.parameters.getParameter(
				ThreeDVisualizerParameters.mzRange).getValue();
		int rtRes = myInstance.parameters.getParameter(
				ThreeDVisualizerParameters.rtResolution).getValue();
		int mzRes = myInstance.parameters.getParameter(
				ThreeDVisualizerParameters.mzResolution).getValue();

		try {
			ThreeDVisualizerWindow newWindow = new ThreeDVisualizerWindow(
					dataFiles[0], msLevel, rtRange, rtRes, mzRange, mzRes);
			MZmineCore.getDesktop().addInternalFrame(newWindow);
		} catch (Error e) {
			// Missing Java3D may cause UnsatisfiedLinkError or
			// NoClassDefFoundError
			String errMsg = "It seems that Java3D is not installed. Please install Java3D and try again.";
			MZmineCore.getDesktop().displayErrorMessage(errMsg);
		}
		return null;
	}

	@Override
	public MZmineModuleCategory getModuleCategory() {
		return MZmineModuleCategory.VISUALIZATIONRAWDATA;
	}

}