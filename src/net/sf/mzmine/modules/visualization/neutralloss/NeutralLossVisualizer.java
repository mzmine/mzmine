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

package net.sf.mzmine.modules.visualization.neutralloss;

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
import net.sf.mzmine.modules.visualization.threed.ThreeDVisualizer;
import net.sf.mzmine.modules.visualization.threed.ThreeDVisualizerParameters;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * Neutral loss (MS/MS) visualizer using JFreeChart library
 */
public class NeutralLossVisualizer implements MZmineModule, ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private static NeutralLossVisualizer myInstance;

	private NeutralLossParameters parameters;

	private Desktop desktop;
	
	private final int neededMSLevels[] = { 1, 2 };


	/**
	 * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
	 */
	public void initModule() {

		this.desktop = MZmineCore.getDesktop();
		
		myInstance = this;

		parameters = new NeutralLossParameters();

		desktop.addMenuItem(MZmineMenu.VISUALIZATIONRAWDATA, "Neutral loss",
				"Plots the neutral loss of each fragment (MS/MS) scan",
				KeyEvent.VK_N, false, this, null);

	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		logger.finest("Opening a new neutral loss visualizer setup dialog");

		RawDataFile dataFiles[] = desktop.getSelectedDataFiles();
		if (dataFiles.length != 1) {
			desktop.displayErrorMessage("Please select one data file");
			return;
		}

		int msLevels[] = dataFiles[0].getMSLevels();
		
		if (!CollectionUtils.isSubset(msLevels, neededMSLevels)) {
			desktop.displayErrorMessage("File " + dataFiles[0]
					+ " does not contain data for MS levels 1 and 2.");
			return;
		}
		
		showNewNeutralLossVisualizerWindow(dataFiles[0]);
	}
	
	public static void showNewNeutralLossVisualizerWindow(RawDataFile dataFile) {
		showNewNeutralLossVisualizerWindow(dataFile, null, null);
	}

	public static void showNewNeutralLossVisualizerWindow(RawDataFile dataFile, Range rtRange, Range mzRange) {

		Hashtable<Parameter, Object> autoValues = null;
		autoValues = new Hashtable<Parameter, Object>();

		autoValues.put(NeutralLossParameters.retentionTimeRange, dataFile
				.getDataRTRange(2));
		autoValues.put(NeutralLossParameters.mzRange, dataFile
				.getDataMZRange(1));

		if (rtRange != null)
			myInstance.parameters.setParameterValue(
					ThreeDVisualizerParameters.retentionTimeRange, rtRange);
		if (mzRange != null)
			myInstance.parameters.setParameterValue(
					ThreeDVisualizerParameters.mzRange, mzRange);

		ParameterSetupDialog dialog = new ParameterSetupDialog(
				"Please set parameter values for Neutral loss visualizer", myInstance.parameters,
				autoValues);

		dialog.setVisible(true);

		if (dialog.getExitCode() != ExitCode.OK)
			return;

		NeutralLossVisualizerWindow newWindow = new NeutralLossVisualizerWindow(
				dataFile, myInstance.parameters);

		MZmineCore.getDesktop().addInternalFrame(newWindow);

	}

	/**
	 * @see net.sf.mzmine.main.MZmineModule#toString()
	 */
	public String toString() {
		return "Neutral loss visualizer";
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
		this.parameters = (NeutralLossParameters) parameters;
	}

}