/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.msms;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineRunnableModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

import com.google.common.collect.Range;

/**
 * MS/MS visualizer using JFreeChart library
 */
public class MsMsVisualizerModule implements MZmineRunnableModule {

    private static final String MODULE_NAME = "MS/MS visualizer";
    private static final String MODULE_DESCRIPTION = "MS/MS visualizer."; // TODO

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
    public ExitCode runModule(@Nonnull MZmineProject project,
	    @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks) {
	RawDataFile dataFiles[] = parameters
		.getParameter(MsMsParameters.dataFiles).getValue()
		.getMatchingRawDataFiles();
	Range<Double> rtRange = parameters.getParameter(
		MsMsParameters.retentionTimeRange).getValue();
	Range<Double> mzRange = parameters.getParameter(MsMsParameters.mzRange)
		.getValue();
	final IntensityType intensityType = parameters.getParameter(
		MsMsParameters.intensityType).getValue();
	final NormalizationType normalizationType = parameters.getParameter(
		MsMsParameters.normalizationType).getValue();
	Double minPeakInt = parameters.getParameter(MsMsParameters.minPeakInt)
		.getValue();
	MsMsVisualizerWindow newWindow = new MsMsVisualizerWindow(dataFiles[0],
		rtRange, mzRange, intensityType, normalizationType, minPeakInt,
		parameters);

	newWindow.setVisible(true);

	return ExitCode.OK;
    }

    public static void showIDAVisualizerSetupDialog(RawDataFile dataFile) {
    	showIDAVisualizerSetupDialog(dataFile, null, null, null, null, 0.0);
    }

    public static void showIDAVisualizerSetupDialog(RawDataFile dataFile,
	    Range<Double> mzRange, Range<Double> rtRange,
	    IntensityType intensityType, NormalizationType normalizationType,
	    Double minPeakInt) {
	ParameterSet parameters = MZmineCore.getConfiguration()
		.getModuleParameters(MsMsVisualizerModule.class);

	parameters.getParameter(MsMsParameters.dataFiles).setValue(
		RawDataFilesSelectionType.SPECIFIC_FILES,
		new RawDataFile[] { dataFile });

	if (rtRange != null)
	    parameters.getParameter(MsMsParameters.retentionTimeRange).setValue(
		    rtRange);
	if (mzRange != null)
	    parameters.getParameter(MsMsParameters.mzRange).setValue(mzRange);
	if (intensityType != null)
	    parameters.getParameter(MsMsParameters.intensityType).setValue(intensityType);
	if (normalizationType != null)
	    parameters.getParameter(MsMsParameters.normalizationType).setValue(normalizationType);
	if (!Double.isNaN(minPeakInt))
	    parameters.getParameter(MsMsParameters.minPeakInt).setValue(minPeakInt);

	ExitCode exitCode = parameters.showSetupDialog(MZmineCore.getDesktop()
		.getMainWindow(), true);

	if (exitCode != ExitCode.OK)
	    return;

	rtRange = parameters.getParameter(MsMsParameters.retentionTimeRange)
		.getValue();
	mzRange = parameters.getParameter(MsMsParameters.mzRange).getValue();
	intensityType = parameters.getParameter(MsMsParameters.intensityType).getValue();
	normalizationType = parameters.getParameter(MsMsParameters.normalizationType).getValue();
	minPeakInt = parameters.getParameter(MsMsParameters.minPeakInt).getValue();

	MsMsVisualizerWindow newWindow = new MsMsVisualizerWindow(dataFile,
		rtRange, mzRange, intensityType, normalizationType, minPeakInt,
		parameters);

	newWindow.setVisible(true);

    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
	return MZmineModuleCategory.VISUALIZATIONRAWDATA;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return MsMsParameters.class;
    }

}