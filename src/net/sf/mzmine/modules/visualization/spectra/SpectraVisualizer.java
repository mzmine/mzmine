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

package net.sf.mzmine.modules.visualization.spectra;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * Spectrum visualizer using JFreeChart library
 */
public class SpectraVisualizer implements MZmineModule, ActionListener {

	private static SpectraVisualizer myInstance;

	private SpectraVisualizerParameters parameters;

	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
	 */
	public void initModule() {

		myInstance = this;

		parameters = new SpectraVisualizerParameters();

		MZmineCore.getDesktop().addMenuItem(MZmineMenu.VISUALIZATIONRAWDATA,
				"Spectra plot", "Mass spectrum visualizer", KeyEvent.VK_S,
				false, this, null);

	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		logger.finest("Opening a new spectra visualizer setup dialog");

		RawDataFile dataFiles[] = MZmineCore.getDesktop()
				.getSelectedDataFiles();
		if (dataFiles.length != 1) {
			MZmineCore.getDesktop().displayErrorMessage(
					"Please select a single data file");
			return;
		}

		showSpectrumVisualizerDialog(dataFiles[0], parameters);

	}

	public static void showSpectrumVisualizerDialog(RawDataFile dataFile) {

		myInstance
				.showSpectrumVisualizerDialog(dataFile, myInstance.parameters);
	}

	private void showSpectrumVisualizerDialog(RawDataFile dataFile,
			SpectraVisualizerParameters parameters) {

		ParameterSetupDialog dialog = new ParameterSetupDialog(
				"Please set parameter values for " + toString(), parameters);

		dialog.setVisible(true);

		if (dialog.getExitCode() != ExitCode.OK)
			return;

		int scanNumber = (Integer) parameters
				.getParameterValue(SpectraVisualizerParameters.scanNumber);

		showNewSpectrumWindow(dataFile, scanNumber);

	}

	public static void showNewSpectrumWindow(RawDataFile dataFile,
			int scanNumber) {
		showNewSpectrumWindow(dataFile, scanNumber, null, null, null);
	}

	public static void showNewSpectrumWindow(RawDataFile dataFile,
			int scanNumber, ChromatographicPeak peak) {
		showNewSpectrumWindow(dataFile, scanNumber, peak, null, null);
	}

	public static void showNewSpectrumWindow(RawDataFile dataFile,
			int scanNumber, IsotopePattern detectedPattern) {
		showNewSpectrumWindow(dataFile, scanNumber, null, detectedPattern, null);
	}

	public static void showNewSpectrumWindow(RawDataFile dataFile,
			int scanNumber, ChromatographicPeak peak, IsotopePattern detectedPattern,
			IsotopePattern predictedPattern) {

		Scan scan = dataFile.getScan(scanNumber);
		
		if (scan == null) {
			MZmineCore.getDesktop().displayErrorMessage(
					"Raw data file " + dataFile + " does not contain scan #"
							+ scanNumber);
			return;
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

	}

	/**
	 * @see net.sf.mzmine.main.MZmineModule#toString()
	 */
	public String toString() {
		return "Spectra visualizer";
	}

	/**
	 * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	/**
	 * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
	 */
	public void setParameters(ParameterSet newParameters) {
		parameters = (SpectraVisualizerParameters) newParameters;
	}

}