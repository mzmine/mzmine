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

package net.sf.mzmine.modules.visualization.tic;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Hashtable;
import java.util.logging.Logger;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;

/**
 * TIC visualizer using JFreeChart library
 */
public class TICVisualizer implements MZmineModule, ActionListener {

    private static TICVisualizer myInstance;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private TICVisualizerParameters parameters;

    private Desktop desktop;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        parameters = new TICVisualizerParameters();

        desktop.addMenuItem(MZmineMenu.VISUALIZATION, "TIC plot", this, null,
                KeyEvent.VK_T, false, true);

        myInstance = this;

    }

    public static TICVisualizer getInstance() {
        return myInstance;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        RawDataFile selectedFiles[] = desktop.getSelectedDataFiles();
        showNewTICVisualizerWindow(selectedFiles, null, parameters);
    }

    public void showNewTICVisualizerWindow(RawDataFile[] dataFiles,
            Peak[] peaks, int msLevel, Object plotType, float rtMin,
            float rtMax, float mzMin, float mzMax) {
        TICVisualizerParameters newParameters = (TICVisualizerParameters) parameters.clone();
        newParameters.setParameterValue(TICVisualizerParameters.msLevel,
                msLevel);
        newParameters.setParameterValue(TICVisualizerParameters.plotType,
                plotType);
        newParameters.setParameterValue(TICVisualizerParameters.minRT, rtMin);
        newParameters.setParameterValue(TICVisualizerParameters.maxRT, rtMax);
        newParameters.setParameterValue(TICVisualizerParameters.minMZ, mzMin);
        newParameters.setParameterValue(TICVisualizerParameters.maxMZ, mzMax);
        showNewTICVisualizerWindow(dataFiles, peaks, newParameters);
    }

    public void showNewTICVisualizerWindow(RawDataFile[] dataFiles, Peak[] peaks) {
        showNewTICVisualizerWindow(dataFiles, peaks, parameters);
    }

    private void showNewTICVisualizerWindow(RawDataFile[] dataFiles,
            Peak[] peaks, TICVisualizerParameters parameters) {

        logger.finest("Opening a new TIC visualizer setup dialog");

        if ((dataFiles == null) || (dataFiles.length == 0)) {
            desktop.displayErrorMessage("Please select at least one data file");
            return;
        }

        Hashtable<Parameter, Object> autoValues = null;
        if (dataFiles.length == 1) {
            autoValues = new Hashtable<Parameter, Object>();
            autoValues.put(TICVisualizerParameters.msLevel, 1);
            autoValues.put(TICVisualizerParameters.minRT,
                    dataFiles[0].getDataMinRT(1));
            autoValues.put(TICVisualizerParameters.maxRT,
                    dataFiles[0].getDataMaxRT(1));
            autoValues.put(TICVisualizerParameters.minMZ,
                    dataFiles[0].getDataMinMZ(1));
            autoValues.put(TICVisualizerParameters.maxMZ,
                    dataFiles[0].getDataMaxMZ(1));
        }

        ParameterSetupDialog dialog = new ParameterSetupDialog(
                "Please set parameter values for " + toString(), parameters,
                autoValues);

        dialog.setVisible(true);

        if (dialog.getExitCode() != ExitCode.OK)
            return;

        int msLevel = (Integer) parameters.getParameterValue(TICVisualizerParameters.msLevel);

        float rtMin = (Float) parameters.getParameterValue(TICVisualizerParameters.minRT);
        float rtMax = (Float) parameters.getParameterValue(TICVisualizerParameters.maxRT);
        float mzMin = (Float) parameters.getParameterValue(TICVisualizerParameters.minMZ);
        float mzMax = (Float) parameters.getParameterValue(TICVisualizerParameters.maxMZ);

        this.parameters = parameters;

        Object plotType = parameters.getParameterValue(TICVisualizerParameters.plotType);

        TICVisualizerWindow newWindow = new TICVisualizerWindow(dataFiles,
                plotType, msLevel, rtMin, rtMax, mzMin, mzMax, peaks);

        desktop.addInternalFrame(newWindow);

    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#toString()
     */
    public String toString() {
        return "TIC visualizer";
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
    public void setParameters(ParameterSet newParameters) {
        parameters = (TICVisualizerParameters) newParameters;
    }

}