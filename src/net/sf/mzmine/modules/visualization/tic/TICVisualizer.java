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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Hashtable;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.visualization.twod.TwoDParameters;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.RawDataFileUtils;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * TIC/XIC visualizer using JFreeChart library
 */
public class TICVisualizer implements MZmineModule, ActionListener {

	private static TICVisualizer myInstance;

	final String helpID = GUIUtils.generateHelpID(this);

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private TICVisualizerParameters parameters;

	private Desktop desktop;

	public TICVisualizer() {

		this.desktop = MZmineCore.getDesktop();

		parameters = new TICVisualizerParameters();

		desktop.addMenuItem(MZmineMenu.VISUALIZATIONRAWDATA, "TIC plot",
				"Visualization of the chromatogram", KeyEvent.VK_T, false,
				this, null);

		myInstance = this;

	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		RawDataFile selectedFiles[] = desktop.getSelectedDataFiles();

		if (selectedFiles.length > 0) {
			myInstance.parameters.getParameter(
					TICVisualizerParameters.dataFiles).setValue(selectedFiles);
		}
		showNewTICVisualizerWindow();
	}

	public static void showNewTICVisualizerWindow(RawDataFile[] dataFiles) {
		myInstance.parameters.getParameter(TICVisualizerParameters.dataFiles)
				.setValue(dataFiles);

		myInstance.showNewTICVisualizerWindow();
	}

	public static void showNewTICVisualizerWindow(RawDataFile[] dataFiles,
			ChromatographicPeak[] peaks, ChromatographicPeak[] selectionPeaks,
			int msLevel, PlotType plotType, Range rtRange, Range mzRange) {

		myInstance.parameters.getParameter(TICVisualizerParameters.msLevel)
				.setValue(msLevel);
		myInstance.parameters.getParameter(TICVisualizerParameters.plotType)
				.setValue(plotType);
		myInstance.parameters.getParameter(
				TICVisualizerParameters.retentionTimeRange).setValue(rtRange);
		myInstance.parameters.getParameter(TICVisualizerParameters.mzRange)
				.setValue(mzRange);
		myInstance.parameters.getParameter(
				TICVisualizerParameters.selectionPeaks).setChoices(peaks);
		myInstance.parameters.getParameter(
				TICVisualizerParameters.selectionPeaks)
				.setValue(selectionPeaks);
		myInstance.parameters.getParameter(TICVisualizerParameters.dataFiles)
				.setValue(dataFiles);

		myInstance.showNewTICVisualizerWindow();
	}

	public static void showNewTICVisualizerWindow(RawDataFile dataFile) {

		myInstance.parameters.getParameter(TICVisualizerParameters.dataFiles)
				.setValue(new RawDataFile[] { dataFile });

		myInstance.showNewTICVisualizerWindow();

	}

	private void showNewTICVisualizerWindow() {

		logger.finest("Opening a new TIC visualizer setup dialog");

		RawDataFile dataFiles[] = MZmineCore.getCurrentProject().getDataFiles();

		assert dataFiles.length > 0;

		MultiChoiceParameter<RawDataFile> p = parameters
				.getParameter(TICVisualizerParameters.dataFiles);
		p.setChoices(dataFiles);

		Integer msLevels[] = CollectionUtils.toIntegerArray(dataFiles[0]
				.getMSLevels());
		myInstance.parameters.getParameter(TwoDParameters.msLevel).setChoices(
				msLevels);

		Hashtable<UserParameter, Object> autoValues = null;
		if (dataFiles.length > 0) {
			autoValues = new Hashtable<UserParameter, Object>();
			autoValues.put(TICVisualizerParameters.msLevel, 1);
			Range rtRange = RawDataFileUtils.findTotalRTRange(dataFiles, 1);
			Range mzRange = RawDataFileUtils.findTotalMZRange(dataFiles, 1);
			autoValues.put(TICVisualizerParameters.retentionTimeRange, rtRange);
			autoValues.put(TICVisualizerParameters.mzRange, mzRange);
		}

		ExitCode exitCode = parameters.showSetupDialog(autoValues);

		if (exitCode != ExitCode.OK)
			return;

		dataFiles = parameters.getParameter(TICVisualizerParameters.dataFiles)
				.getValue();
		int msLevel = parameters.getParameter(TICVisualizerParameters.msLevel)
				.getValue();
		Range rtRange = parameters.getParameter(
				TICVisualizerParameters.retentionTimeRange).getValue();
		Range mzRange = parameters
				.getParameter(TICVisualizerParameters.mzRange).getValue();
		ChromatographicPeak selectionPeaks[] = parameters.getParameter(
				TICVisualizerParameters.selectionPeaks).getValue();
		PlotType plotType = parameters.getParameter(
				TICVisualizerParameters.plotType).getValue();

		if (dataFiles.length == 0) {
			desktop.displayErrorMessage("Please select at least one data file");
			return;
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
			desktop.addInternalFrame(newWindow);
		} else {
			desktop.displayErrorMessage("No scans found at MS level " + msLevel
					+ " within given retention time range.");
		}

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

}