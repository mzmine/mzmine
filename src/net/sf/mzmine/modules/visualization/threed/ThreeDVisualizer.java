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

package net.sf.mzmine.modules.visualization.threed;

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
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * 3D visualizer module
 */
public class ThreeDVisualizer implements MZmineModule, ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
    final String helpID = GUIUtils.generateHelpID(this);

	private static ThreeDVisualizer myInstance;

	private ThreeDVisualizerParameters parameters;

	private Desktop desktop;

	/**
	 * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
	 */
	public void initModule() {

		this.desktop = MZmineCore.getDesktop();

		myInstance = this;

		parameters = new ThreeDVisualizerParameters();

		desktop.addMenuItem(MZmineMenu.VISUALIZATIONRAWDATA, "3D plot",
				"3D visualization (requires Java3D)", KeyEvent.VK_3, false,
				this, null);

	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		logger.finest("Opening a new 3D visualizer setup dialog");

		RawDataFile dataFiles[] = desktop.getSelectedDataFiles();
		if (dataFiles.length != 1) {
			desktop.displayErrorMessage("Please select a single data file");
			return;
		}

		show3DVisualizerSetupDialog(dataFiles[0]);

	}

	/**
	 * @see net.sf.mzmine.main.MZmineModule#toString()
	 */
	public String toString() {
		return "3D visualizer";
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
		this.parameters = (ThreeDVisualizerParameters) parameters;
	}

	public static void show3DVisualizerSetupDialog(RawDataFile dataFile) {
		show3DVisualizerSetupDialog(dataFile, null, null);
	}

	public static void show3DVisualizerSetupDialog(RawDataFile dataFile,
			Range mzRange, Range rtRange) {

		Hashtable<Parameter, Object> autoValues = new Hashtable<Parameter, Object>();
		autoValues.put(ThreeDVisualizerParameters.msLevel, 1);
		autoValues.put(ThreeDVisualizerParameters.retentionTimeRange, dataFile
				.getDataRTRange(1));
		autoValues.put(ThreeDVisualizerParameters.mzRange, dataFile
				.getDataMZRange(1));

		if (rtRange != null)
			myInstance.parameters.setParameterValue(
					ThreeDVisualizerParameters.retentionTimeRange, rtRange);
		if (mzRange != null)
			myInstance.parameters.setParameterValue(
					ThreeDVisualizerParameters.mzRange, mzRange);

		ParameterSetupDialog dialog = new ParameterSetupDialog(
				"Please set parameter values for 3D visualizer",
				myInstance.parameters, autoValues, myInstance.helpID);

		dialog.setVisible(true);

		if (dialog.getExitCode() != ExitCode.OK)
			return;

		int msLevel = (Integer) myInstance.parameters
				.getParameterValue(ThreeDVisualizerParameters.msLevel);
		rtRange = (Range) myInstance.parameters
				.getParameterValue(ThreeDVisualizerParameters.retentionTimeRange);
		mzRange = (Range) myInstance.parameters
				.getParameterValue(ThreeDVisualizerParameters.mzRange);
		int rtRes = (Integer) myInstance.parameters
				.getParameterValue(ThreeDVisualizerParameters.rtResolution);
		int mzRes = (Integer) myInstance.parameters
				.getParameterValue(ThreeDVisualizerParameters.mzResolution);

		try {
			ThreeDVisualizerWindow newWindow = new ThreeDVisualizerWindow(
					dataFile, msLevel, rtRange, rtRes, mzRange, mzRes);
			MZmineCore.getDesktop().addInternalFrame(newWindow);
		} catch (UnsatisfiedLinkError e) {
			MZmineCore
					.getDesktop()
					.displayErrorMessage(
							"It seems that Java3D is not installed. Please install Java3D and try again.");
		}


	}

}