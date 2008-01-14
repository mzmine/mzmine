/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
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
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;

/**
 * 3D visualizer using VisAD library
 */
public class ThreeDVisualizer implements MZmineModule, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private ThreeDVisualizerParameters parameters;

    private Desktop desktop;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        parameters = new ThreeDVisualizerParameters();

        desktop.addMenuItem(MZmineMenu.VISUALIZATION, "3D plot", this, null,
                KeyEvent.VK_3, false, true);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        logger.finest("Opening a new 3D visualizer setup dialog");

        RawDataFile dataFiles[] = desktop.getSelectedDataFiles();
        if (dataFiles.length != 1) {
            desktop.displayErrorMessage("Please select a single file");
            return;
        }

        Hashtable<Parameter, Object> autoValues = new Hashtable<Parameter, Object>();
        autoValues.put(ThreeDVisualizerParameters.msLevel, 1);
        autoValues.put(ThreeDVisualizerParameters.minRT,
                dataFiles[0].getDataMinRT(1));
        autoValues.put(ThreeDVisualizerParameters.maxRT,
                dataFiles[0].getDataMaxRT(1));
        autoValues.put(ThreeDVisualizerParameters.minMZ,
                dataFiles[0].getDataMinMZ(1));
        autoValues.put(ThreeDVisualizerParameters.maxMZ,
                dataFiles[0].getDataMaxMZ(1));

        ParameterSetupDialog dialog = new ParameterSetupDialog(
                "Please set parameter values for " + toString(), parameters,
                autoValues);

        dialog.setVisible(true);

        if (dialog.getExitCode() != ExitCode.OK)
            return;

        int msLevel = (Integer) parameters.getParameterValue(ThreeDVisualizerParameters.msLevel);
        float rtMin = (Float) parameters.getParameterValue(ThreeDVisualizerParameters.minRT);
        float rtMax = (Float) parameters.getParameterValue(ThreeDVisualizerParameters.maxRT);
        float mzMin = (Float) parameters.getParameterValue(ThreeDVisualizerParameters.minMZ);
        float mzMax = (Float) parameters.getParameterValue(ThreeDVisualizerParameters.maxMZ);
        int rtRes = (Integer) parameters.getParameterValue(ThreeDVisualizerParameters.rtResolution);
        int mzRes = (Integer) parameters.getParameterValue(ThreeDVisualizerParameters.mzResolution);

        ThreeDVisualizerWindow newWindow = new ThreeDVisualizerWindow(
                dataFiles[0], msLevel, rtMin, rtMax, mzMin, mzMax, rtRes, mzRes);

        desktop.addInternalFrame(newWindow);

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

}