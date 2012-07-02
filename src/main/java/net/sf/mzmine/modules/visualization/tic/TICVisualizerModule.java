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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.tic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.Range;

/**
 * TIC/XIC visualizer using JFreeChart library
 */
public class TICVisualizerModule implements MZmineProcessingModule {

    private static final String MODULE_NAME = "TIC/XIC visualizer";
    private static final String MODULE_DESCRIPTION = "TIC/XIC visualizer."; // TODO

    @Override
    public @Nonnull String getName() {
	return MODULE_NAME;
    }

    @Override
    public @Nonnull String getDescription() {
	return MODULE_DESCRIPTION;
    }

    @Override
    @Nonnull
    public ExitCode runModule(@Nonnull ParameterSet parameters,
	    @Nonnull Collection<Task> tasks) {
	final RawDataFile[] dataFiles = parameters.getParameter(
		TICVisualizerParameters.DATA_FILES).getValue();
	final int msLevel = parameters.getParameter(
		TICVisualizerParameters.MS_LEVEL).getValue();
	final Range rtRange = parameters.getParameter(
		TICVisualizerParameters.RT_RANGE).getValue();
	final Range mzRange = parameters.getParameter(
		TICVisualizerParameters.MZ_RANGE).getValue();
	final PlotType plotType = parameters.getParameter(
		TICVisualizerParameters.PLOT_TYPE).getValue();
	final ChromatographicPeak[] selectionPeaks = parameters.getParameter(
		TICVisualizerParameters.PEAKS).getValue();

	if (dataFiles == null || dataFiles.length == 0) {

	    MZmineCore.getDesktop().displayErrorMessage(
		    "Please select raw data file(s)");

	} else {

	    // Add the window to the desktop only if we actually have any raw
	    // data to show.
	    boolean weHaveData = false;
	    for (int i = 0, dataFilesLength = dataFiles.length; !weHaveData
		    && i < dataFilesLength; i++) {

		weHaveData = dataFiles[i].getScanNumbers(msLevel, rtRange).length > 0;
	    }

	    if (weHaveData) {

		MZmineCore.getDesktop().addInternalFrame(
			new TICVisualizerWindow(dataFiles, plotType, msLevel,
				rtRange, mzRange, selectionPeaks,
				((TICVisualizerParameters) parameters)
					.getPeakLabelMap()));

	    } else {

		MZmineCore.getDesktop().displayErrorMessage(
			"No scans found at MS level " + msLevel
				+ " within given retention time range.");
	    }
	}

	return ExitCode.OK;
    }

    public static void setupNewTICVisualizer(final RawDataFile dataFile) {

	setupNewTICVisualizer(new RawDataFile[] { dataFile });
    }

    public static void setupNewTICVisualizer(final RawDataFile[] dataFiles) {

	setupNewTICVisualizer(MZmineCore.getCurrentProject().getDataFiles(),
		dataFiles, new ChromatographicPeak[0],
		new ChromatographicPeak[0], null, null, null);
    }

    public static void setupNewTICVisualizer(final RawDataFile[] allFiles,
	    final RawDataFile[] selectedFiles,
	    final ChromatographicPeak[] allPeaks,
	    final ChromatographicPeak[] selectedPeaks,
	    final Map<ChromatographicPeak, String> peakLabels,
	    final Range rtRange, final Range mzRange) {

	assert allFiles != null;

	final TICVisualizerModule myInstance = MZmineCore.getModuleInstance(TICVisualizerModule.class);
	final TICVisualizerParameters myParameters = (TICVisualizerParameters) MZmineCore.getConfiguration().getModuleParameters(TICVisualizerModule.class);
	myParameters.getParameter(TICVisualizerParameters.MS_LEVEL).setValue(1);
	myParameters.getParameter(TICVisualizerParameters.PLOT_TYPE).setValue(
		PlotType.BASEPEAK);

	if (rtRange != null) {

	    myParameters.getParameter(TICVisualizerParameters.RT_RANGE)
		    .setValue(rtRange);
	}

	if (mzRange != null) {

	    myParameters.getParameter(TICVisualizerParameters.MZ_RANGE)
		    .setValue(mzRange);
	}

	if (myParameters.showSetupDialog(allFiles, selectedFiles, allPeaks,
		selectedPeaks) == ExitCode.OK) {

	    final TICVisualizerParameters p = (TICVisualizerParameters) myParameters
		    .cloneParameter();

	    if (peakLabels != null) {
		p.setPeakLabelMap(peakLabels);
	    }

	    myInstance.runModule(p, new ArrayList<Task>());
	}
    }

    public static void showNewTICVisualizerWindow(
	    final RawDataFile[] dataFiles,
	    final ChromatographicPeak[] selectionPeaks,
	    final Map<ChromatographicPeak, String> peakLabels,
	    final int msLevel, final PlotType plotType, final Range rtRange,
	    final Range mzRange) {

	MZmineCore.getDesktop().addInternalFrame(
		new TICVisualizerWindow(dataFiles, plotType, msLevel, rtRange,
			mzRange, selectionPeaks, peakLabels));
    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
	return MZmineModuleCategory.VISUALIZATIONRAWDATA;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return TICVisualizerParameters.class;
    }
}