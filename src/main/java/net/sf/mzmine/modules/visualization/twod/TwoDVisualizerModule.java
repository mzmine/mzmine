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

package net.sf.mzmine.modules.visualization.twod;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.Range;

/**
 * 2D visualizer using JFreeChart library
 */
public class TwoDVisualizerModule implements MZmineProcessingModule {

    private static final String MODULE_NAME = "2D visualizer";
    private static final String MODULE_DESCRIPTION = "2D visualizer."; // TODO

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
	RawDataFile dataFiles[] = parameters.getParameter(
		TwoDParameters.dataFiles).getValue();

	if ((dataFiles == null) || (dataFiles.length == 0)) {
	    MZmineCore.getDesktop().displayErrorMessage(
		    "Please select raw data file");
	    return ExitCode.ERROR;
	}

	int msLevel = parameters.getParameter(TwoDParameters.msLevel)
		.getValue();
	Range rtRange = parameters.getParameter(
		TwoDParameters.retentionTimeRange).getValue();
	Range mzRange = parameters.getParameter(TwoDParameters.mzRange)
		.getValue();
	TwoDVisualizerWindow newWindow = new TwoDVisualizerWindow(dataFiles[0],
		msLevel, rtRange, mzRange, parameters);

	MZmineCore.getDesktop().addInternalFrame(newWindow);

	return ExitCode.OK;
    }

    public static void show2DVisualizerSetupDialog(RawDataFile dataFile) {
	show2DVisualizerSetupDialog(dataFile, null, null);
    }

    public static void show2DVisualizerSetupDialog(RawDataFile dataFile,
	    Range mzRange, Range rtRange) {

	ParameterSet parameters = MZmineCore.getConfiguration()
		.getModuleParameters(TwoDVisualizerModule.class);

	parameters.getParameter(TwoDParameters.dataFiles).setValue(
		new RawDataFile[] { dataFile });

	if (rtRange != null)
	    parameters.getParameter(TwoDParameters.retentionTimeRange)
		    .setValue(rtRange);
	if (mzRange != null)
	    parameters.getParameter(TwoDParameters.mzRange).setValue(mzRange);

	ExitCode exitCode = parameters.showSetupDialog();

	if (exitCode != ExitCode.OK)
	    return;

	int msLevel = parameters.getParameter(TwoDParameters.msLevel)
		.getValue();
	rtRange = parameters.getParameter(TwoDParameters.retentionTimeRange)
		.getValue();
	mzRange = parameters.getParameter(TwoDParameters.mzRange).getValue();

	TwoDVisualizerWindow newWindow = new TwoDVisualizerWindow(dataFile,
		msLevel, rtRange, mzRange, parameters);

	MZmineCore.getDesktop().addInternalFrame(newWindow);

    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
	return MZmineModuleCategory.VISUALIZATIONRAWDATA;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return TwoDParameters.class;
    }

}