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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.tic;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * TIC/XIC visualizer using JFreeChart library
 */
public class TICVisualizerModule implements MZmineProcessingModule {

	private static TICVisualizerModule myInstance;

	private TICVisualizerParameters parameters = new TICVisualizerParameters();

	public TICVisualizerModule() {
		myInstance = this;
	}

	public static void setupNewTICVisualizer(RawDataFile dataFile) {
		setupNewTICVisualizer(new RawDataFile[] { dataFile });
	}

	public static void setupNewTICVisualizer(RawDataFile[] dataFiles) {
		setupNewTICVisualizer(MZmineCore.getCurrentProject().getDataFiles(),
				dataFiles, new ChromatographicPeak[0],
				new ChromatographicPeak[0], null, null);
	}

	public static void setupNewTICVisualizer(RawDataFile[] allFiles,
			RawDataFile[] selectedFiles, ChromatographicPeak allPeaks[],
			ChromatographicPeak selectedPeaks[], Range rtRange, Range mzRange) {

		assert allFiles != null;

		myInstance.parameters.getParameter(TICVisualizerParameters.msLevel)
				.setValue(1);

		myInstance.parameters.getParameter(TICVisualizerParameters.plotType)
				.setValue(PlotType.BASEPEAK);

		if (rtRange != null)
			myInstance.parameters.getParameter(
					TICVisualizerParameters.retentionTimeRange).setValue(
					rtRange);
		if (mzRange != null)
			myInstance.parameters.getParameter(TICVisualizerParameters.mzRange)
					.setValue(mzRange);

		ExitCode exitCode = myInstance.parameters.showSetupDialog(allFiles,
				selectedFiles, allPeaks, selectedPeaks);

		if (exitCode == ExitCode.OK)
			myInstance.runModule(myInstance.parameters.clone());
	}

	public static void showNewTICVisualizerWindow(RawDataFile[] dataFiles,
			ChromatographicPeak selectionPeaks[], int msLevel,
			PlotType plotType, Range rtRange, Range mzRange) {

		TICVisualizerWindow newWindow = new TICVisualizerWindow(dataFiles,
				plotType, msLevel, rtRange, mzRange, selectionPeaks);
		MZmineCore.getDesktop().addInternalFrame(newWindow);

	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#toString()
	 */
	public String toString() {
		return "TIC/XIC visualizer";
	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#getParameterSet()
	 */
	public TICVisualizerParameters getParameterSet() {
		return parameters;
	}

	@Override
	public Task[] runModule(ParameterSet parameters) {

		RawDataFile dataFiles[] = parameters.getParameter(
				TICVisualizerParameters.dataFiles).getValue();
		int msLevel = parameters.getParameter(TICVisualizerParameters.msLevel)
				.getValue();
		Range rtRange = parameters.getParameter(
				TICVisualizerParameters.retentionTimeRange).getValue();
		Range mzRange = parameters
				.getParameter(TICVisualizerParameters.mzRange).getValue();
		PlotType plotType = parameters.getParameter(
				TICVisualizerParameters.plotType).getValue();
		ChromatographicPeak selectionPeaks[] = parameters.getParameter(
				TICVisualizerParameters.peaks).getValue();

		if ((dataFiles == null) || (dataFiles.length == 0)) {
			MZmineCore.getDesktop().displayErrorMessage(
					"Please select raw data file(s)");
			return null;
		}

		// Add the window to the desktop only if we actually have any raw data
		// to show
		boolean weHaveData = false;
		for (RawDataFile file : dataFiles) {
			int scanNumbers[] = file.getScanNumbers(msLevel, rtRange);
			if (scanNumbers.length > 0) {
				weHaveData = true;
				break;
			}
		}
		if (weHaveData) {
			TICVisualizerWindow newWindow = new TICVisualizerWindow(dataFiles,
					plotType, msLevel, rtRange, mzRange, selectionPeaks);
			MZmineCore.getDesktop().addInternalFrame(newWindow);
		} else {
			MZmineCore.getDesktop().displayErrorMessage(
					"No scans found at MS level " + msLevel
							+ " within given retention time range.");
		}
		return null;
	}

	@Override
	public MZmineModuleCategory getModuleCategory() {
		return MZmineModuleCategory.VISUALIZATIONRAWDATA;
	}

}