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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Hashtable;
import java.util.logging.Logger;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.visualization.twod.TwoDParameters;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * 3D visualizer module
 */
public class ThreeDVisualizer implements MZmineModule, ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	final String helpID = GUIUtils.generateHelpID(this);

	private static ThreeDVisualizer myInstance;

	private ThreeDVisualizerParameters parameters;

	private Desktop desktop;

	public ThreeDVisualizer() {

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

	public static void show3DVisualizerSetupDialog(RawDataFile dataFile) {
		show3DVisualizerSetupDialog(dataFile, null, null);
	}

	public static void show3DVisualizerSetupDialog(RawDataFile dataFile,
			Range mzRange, Range rtRange) {

		Hashtable<UserParameter, Object> autoValues = new Hashtable<UserParameter, Object>();
		autoValues.put(ThreeDVisualizerParameters.msLevel, 1);
		autoValues.put(ThreeDVisualizerParameters.retentionTimeRange,
				dataFile.getDataRTRange(1));
		autoValues.put(ThreeDVisualizerParameters.mzRange,
				dataFile.getDataMZRange(1));

		if (rtRange != null)
			myInstance.parameters.getParameter(
					ThreeDVisualizerParameters.retentionTimeRange).setValue(
					rtRange);
		if (mzRange != null)
			myInstance.parameters.getParameter(
					ThreeDVisualizerParameters.mzRange).setValue(mzRange);

		Integer msLevels[] = CollectionUtils.toIntegerArray(dataFile
				.getMSLevels());
		myInstance.parameters.getParameter(TwoDParameters.msLevel).setChoices(
				msLevels);

		ExitCode exitCode = myInstance.parameters.showSetupDialog(autoValues);

		if (exitCode != ExitCode.OK)
			return;

		int msLevel = myInstance.parameters.getParameter(
				ThreeDVisualizerParameters.msLevel).getValue();
		rtRange = myInstance.parameters.getParameter(
				ThreeDVisualizerParameters.retentionTimeRange).getValue();
		mzRange = myInstance.parameters.getParameter(
				ThreeDVisualizerParameters.mzRange).getValue();
		int rtRes = myInstance.parameters.getParameter(
				ThreeDVisualizerParameters.rtResolution).getInt();
		int mzRes = myInstance.parameters.getParameter(
				ThreeDVisualizerParameters.mzResolution).getInt();

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