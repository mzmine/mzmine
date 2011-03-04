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
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * 2D visualizer using JFreeChart library
 */
public class TwoDVisualizer implements MZmineModule, ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	final String helpID = GUIUtils.generateHelpID(this);

	private static TwoDVisualizer myInstance;

	private TwoDParameters parameters;

	private Desktop desktop;

	public TwoDVisualizer() {

		myInstance = this;

		this.desktop = MZmineCore.getDesktop();

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

		Hashtable<UserParameter, Object> autoValues = new Hashtable<UserParameter, Object>();
		autoValues.put(TwoDParameters.msLevel, 1);
		autoValues.put(TwoDParameters.retentionTimeRange,
				dataFile.getDataRTRange(1));
		autoValues.put(TwoDParameters.mzRange, dataFile.getDataMZRange(1));

		if (rtRange != null)
			myInstance.parameters.getParameter(
					TwoDParameters.retentionTimeRange).setValue(rtRange);
		if (mzRange != null)
			myInstance.parameters.getParameter(TwoDParameters.mzRange)
					.setValue(mzRange);

		Integer msLevels[] = CollectionUtils.toIntegerArray(dataFile
				.getMSLevels());
		myInstance.parameters.getParameter(TwoDParameters.msLevel).setChoices(
				msLevels);

		ExitCode exitCode = myInstance.parameters.showSetupDialog(autoValues);

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

}