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

package net.sf.mzmine.modules.visualization.threed;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.Range;
import visad.VisADException;

/**
 * 3D visualizer module
 */
public class ThreeDVisualizerModule implements MZmineProcessingModule {

    private static final Logger LOG = Logger
	    .getLogger(ThreeDVisualizerModule.class.getName());

    private static final String MODULE_NAME = "3D visualizer";
    private static final String MODULE_DESCRIPTION = "3D visualizer."; // TODO

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
		ThreeDVisualizerParameters.dataFiles).getValue();
	final int msLevel = parameters.getParameter(
		ThreeDVisualizerParameters.msLevel).getValue();
	final Range rtRange = parameters.getParameter(
		ThreeDVisualizerParameters.retentionTimeRange).getValue();

	final Desktop desktop = MZmineCore.getDesktop();

	// Check scan numbers.
	final RawDataFile dataFile = dataFiles[0];
	if (dataFile.getScanNumbers(msLevel, rtRange).length == 0) {

	    desktop.displayErrorMessage("No scans found at MS level " + msLevel
		    + " within given retention time range.");
	    return ExitCode.ERROR;

	}

	ParameterSet myParameters = MZmineCore.getConfiguration()
		.getModuleParameters(ThreeDVisualizerModule.class);
	try {
	    desktop.addInternalFrame(new ThreeDVisualizerWindow(
		    dataFile,
		    msLevel,
		    rtRange,
		    myParameters.getParameter(
			    ThreeDVisualizerParameters.rtResolution).getValue(),
		    myParameters.getParameter(
			    ThreeDVisualizerParameters.mzRange).getValue(),
		    myParameters.getParameter(
			    ThreeDVisualizerParameters.mzResolution).getValue()));
	} catch (RemoteException e) {

	    final String msg = "Couldn't create 3D plot";
	    LOG.log(Level.WARNING, msg, e);
	    desktop.displayErrorMessage(msg);
	} catch (VisADException e) {

	    final String msg = "Couldn't create 3D plot";
	    LOG.log(Level.WARNING, msg, e);
	    desktop.displayErrorMessage(msg);
	} catch (Error e) {

	    // Missing Java3D may cause UnsatisfiedLinkError or
	    // NoClassDefFoundError.
	    final String msg = "It seems that Java3D is not installed. Please install Java3D and try again.";
	    LOG.log(Level.WARNING, msg, e);
	    desktop.displayErrorMessage(msg);
	}

	return ExitCode.OK;

    }

    public static void setupNew3DVisualizer(final RawDataFile dataFile) {
	setupNew3DVisualizer(dataFile, null, null);
    }

    public static void setupNew3DVisualizer(final RawDataFile dataFile,
	    final Range mzRange, final Range rtRange) {

	final ParameterSet myParameters = MZmineCore.getConfiguration()
		.getModuleParameters(ThreeDVisualizerModule.class);
	final ThreeDVisualizerModule myInstance = MZmineCore
		.getModuleInstance(ThreeDVisualizerModule.class);
	myParameters.getParameter(ThreeDVisualizerParameters.dataFiles)
		.setValue(new RawDataFile[] { dataFile });
	myParameters
		.getParameter(ThreeDVisualizerParameters.retentionTimeRange)
		.setValue(rtRange);
	myParameters.getParameter(ThreeDVisualizerParameters.mzRange).setValue(
		mzRange);
	if (myParameters.showSetupDialog() == ExitCode.OK) {
	    myInstance.runModule(myParameters.cloneParameter(), new ArrayList<Task>());
	}
    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
	return MZmineModuleCategory.VISUALIZATIONRAWDATA;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return ThreeDVisualizerParameters.class;
    }
}