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

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.ExitCode;
import visad.VisADException;

import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 3D visualizer module
 */
public class ThreeDVisualizerModule implements MZmineProcessingModule {

    private static final Logger LOG = Logger.getLogger(ThreeDVisualizerModule.class.getName());

    private static ThreeDVisualizerModule myInstance;

    private final ThreeDVisualizerParameters parameters = new ThreeDVisualizerParameters();

    public ThreeDVisualizerModule() {
        myInstance = this;
    }

    public String toString() {
        return "3D visualizer";
    }

    @Override
    public ParameterSet getParameterSet() {
        return parameters;
    }

    public static void setupNew3DVisualizer(final RawDataFile dataFile) {
        setupNew3DVisualizer(dataFile, null, null);
    }

    public static void setupNew3DVisualizer(final RawDataFile dataFile, final Range mzRange, final Range rtRange) {

        final ThreeDVisualizerParameters myParameters = myInstance.parameters;
        myParameters.getParameter(ThreeDVisualizerParameters.dataFiles).setValue(new RawDataFile[]{dataFile});
        myParameters.getParameter(ThreeDVisualizerParameters.retentionTimeRange).setValue(rtRange);
        myParameters.getParameter(ThreeDVisualizerParameters.mzRange).setValue(mzRange);
        if (myParameters.showSetupDialog() == ExitCode.OK) {
            myInstance.runModule(myParameters.clone());
        }
    }

    @Override
    public Task[] runModule(final ParameterSet parameterSet) {

        final ThreeDVisualizerParameters myParameters = myInstance.parameters;
        final RawDataFile[] dataFiles = myParameters.getParameter(ThreeDVisualizerParameters.dataFiles).getValue();
        final int msLevel = myParameters.getParameter(ThreeDVisualizerParameters.msLevel).getValue();
        final Range rtRange = myParameters.getParameter(ThreeDVisualizerParameters.retentionTimeRange).getValue();

        final Desktop desktop = MZmineCore.getDesktop();

        // Check data files.
        if (dataFiles == null || dataFiles.length == 0) {

            desktop.displayErrorMessage("Please select raw data file");

        } else {

            // Check scan numbers.
            final RawDataFile dataFile = dataFiles[0];
            if (dataFile.getScanNumbers(msLevel, rtRange).length == 0) {

                desktop.displayErrorMessage(
                        "No scans found at MS level " + msLevel + " within given retention time range.");

            } else {

                try {
                    desktop.addInternalFrame(new ThreeDVisualizerWindow(
                            dataFile,
                            msLevel,
                            rtRange,
                            myParameters.getParameter(ThreeDVisualizerParameters.rtResolution).getValue(),
                            myParameters.getParameter(ThreeDVisualizerParameters.mzRange).getValue(),
                            myParameters.getParameter(ThreeDVisualizerParameters.mzResolution).getValue()));
                }
                catch (RemoteException e) {

                    final String msg = "Couldn't create 3D plot";
                    LOG.log(Level.WARNING, msg, e);
                    desktop.displayErrorMessage(msg);
                }
                catch (VisADException e) {

                    final String msg = "Couldn't create 3D plot";
                    LOG.log(Level.WARNING, msg, e);
                    desktop.displayErrorMessage(msg);
                }
                catch (Error e) {

                    // Missing Java3D may cause UnsatisfiedLinkError or NoClassDefFoundError.
                    final String msg = "It seems that Java3D is not installed. Please install Java3D and try again.";
                    LOG.log(Level.WARNING, msg, e);
                    desktop.displayErrorMessage(msg);
                }
            }
        }
        return null;
    }

    @Override
    public MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.VISUALIZATIONRAWDATA;
    }
}