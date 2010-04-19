/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Hashtable;
import java.util.logging.Logger;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * 2D visualizer using JFreeChart library
 */
public class TwoDVisualizer implements MZmineModule, ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private static TwoDVisualizer myInstance;

	private TwoDParameters parameters;

	private static PeakThresholdParameters peakThresholdParameters;

	private Desktop desktop;

	/**
	 * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
	 */
	public void initModule() {

		myInstance = this;

		this.desktop = MZmineCore.getDesktop();

		peakThresholdParameters = new PeakThresholdParameters();

		parameters = new TwoDParameters();

		desktop.addMenuItem(MZmineMenu.VISUALIZATIONRAWDATA, "2D plot",
				"2D visualization", KeyEvent.VK_2, false, this, null);

	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		logger.finest("Opening a new 2D visualizer setup dialog");

		RawDataFile dataFiles[] = desktop.getSelectedDataFiles();
		if (dataFiles.length != 1) {
			desktop.displayErrorMessage("Please select a single data file");
			return;
		}

		show2DVisualizerSetupDialog(dataFiles[0], null, null);

	}

	/**
	 * @see net.sf.mzmine.main.MZmineModule#toString()
	 */
	public String toString() {
		return "2D visualizer";
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
	public void setParameters(ParameterSet parameters) {
		this.parameters = (TwoDParameters) parameters;
	}

	static PeakThresholdParameters getThresholdParameters() {
		return peakThresholdParameters;
	}

	public static void show2DVisualizerSetupDialog(RawDataFile dataFile) {
		show2DVisualizerSetupDialog(dataFile, null, null);
	}

	public static void show2DVisualizerSetupDialog(RawDataFile dataFile,
			Range mzRange, Range rtRange) {

		Hashtable<Parameter, Object> autoValues = new Hashtable<Parameter, Object>();
		autoValues.put(TwoDParameters.msLevel, 1);
		autoValues.put(TwoDParameters.retentionTimeRange, dataFile
				.getDataRTRange(1));
		autoValues.put(TwoDParameters.mzRange, dataFile.getDataMZRange(1));

		if (rtRange != null)
			myInstance.parameters.setParameterValue(
					TwoDParameters.retentionTimeRange, rtRange);
		if (mzRange != null)
			myInstance.parameters.setParameterValue(TwoDParameters.mzRange,
					mzRange);

		ParameterSetupDialog dialog = new ParameterSetupDialog(
				"Please set parameter values for 2D visualizer",
				myInstance.parameters, autoValues);

		dialog.setVisible(true);

		if (dialog.getExitCode() != ExitCode.OK)
			return;

		int msLevel = (Integer) myInstance.parameters
				.getParameterValue(TwoDParameters.msLevel);
		rtRange = (Range) myInstance.parameters
				.getParameterValue(TwoDParameters.retentionTimeRange);
		mzRange = (Range) myInstance.parameters
				.getParameterValue(TwoDParameters.mzRange);

		TwoDVisualizerWindow newWindow = new TwoDVisualizerWindow(dataFile,
				msLevel, rtRange, mzRange, peakThresholdParameters);

		MZmineCore.getDesktop().addInternalFrame(newWindow);

	}

}