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

package net.sf.mzmine.modules.visualization.spectra;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.visualization.threed.ThreeDVisualizerParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * Spectrum visualizer using JFreeChart library
 */
public class SpectraVisualizerModule implements MZmineProcessingModule {

	private static SpectraVisualizerModule myInstance;

	private SpectraVisualizerParameters parameters = new SpectraVisualizerParameters();

	public SpectraVisualizerModule() {
		myInstance = this;
	}

	public static void setupNewSpectrumVisualizer(RawDataFile dataFile) {

		myInstance.parameters
				.getParameter(ThreeDVisualizerParameters.dataFiles).setValue(
						new RawDataFile[] { dataFile });

		ExitCode exitCode = myInstance.parameters.showSetupDialog();

		if (exitCode == ExitCode.OK)
			myInstance.runModule(myInstance.parameters.clone());

	}

	public static SpectraVisualizerWindow showNewSpectrumWindow(
			RawDataFile dataFile, int scanNumber) {
		return showNewSpectrumWindow(dataFile, scanNumber, null, null, null);
	}

	public static SpectraVisualizerWindow showNewSpectrumWindow(
			RawDataFile dataFile, int scanNumber, ChromatographicPeak peak) {
		return showNewSpectrumWindow(dataFile, scanNumber, peak, null, null);
	}

	public static SpectraVisualizerWindow showNewSpectrumWindow(
			RawDataFile dataFile, int scanNumber, IsotopePattern detectedPattern) {
		return showNewSpectrumWindow(dataFile, scanNumber, null,
				detectedPattern, null);
	}

	public static SpectraVisualizerWindow showNewSpectrumWindow(
			RawDataFile dataFile, int scanNumber, ChromatographicPeak peak,
			IsotopePattern detectedPattern, IsotopePattern predictedPattern) {

		Scan scan = dataFile.getScan(scanNumber);

		if (scan == null) {
			MZmineCore.getDesktop().displayErrorMessage(
					"Raw data file " + dataFile + " does not contain scan #"
							+ scanNumber);
			return null;
		}

		SpectraVisualizerWindow newWindow = new SpectraVisualizerWindow(
				dataFile);
		newWindow.loadRawData(scan);

		if (peak != null)
			newWindow.loadSinglePeak(peak);

		if (detectedPattern != null)
			newWindow.loadIsotopes(detectedPattern);

		if (predictedPattern != null)
			newWindow.loadIsotopes(predictedPattern);

		MZmineCore.getDesktop().addInternalFrame(newWindow);

		return newWindow;

	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#toString()
	 */
	public String toString() {
		return "Spectra visualizer";
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
	public void setParameters(ParameterSet newParameters) {
		parameters = (SpectraVisualizerParameters) newParameters;
	}

	@Override
	public Task[] runModule(ParameterSet parameters) {

		RawDataFile dataFiles[] = parameters.getParameter(
				SpectraVisualizerParameters.dataFiles).getValue();
		
		if ((dataFiles == null) || (dataFiles.length == 0)) {
			MZmineCore.getDesktop().displayErrorMessage(
					"Please select raw data file");
			return null;
		}

		int scanNumber = parameters.getParameter(
				SpectraVisualizerParameters.scanNumber).getValue();

		showNewSpectrumWindow(dataFiles[0], scanNumber);

		return null;
	}

	@Override
	public MZmineModuleCategory getModuleCategory() {
		return MZmineModuleCategory.VISUALIZATIONRAWDATA;
	}

}