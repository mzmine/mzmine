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
import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * TIC/XIC visualizer using JFreeChart library
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

        desktop.addMenuItem(MZmineMenu.VISUALIZATIONRAWDATA, "TIC plot",
                "Visualization of the chromatogram", KeyEvent.VK_T, false, this, null);

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
            ChromatographicPeak[] peaks, int msLevel, Object plotType,
            Range rtRange, Range mzRange) {
        TICVisualizerParameters newParameters = (TICVisualizerParameters) parameters.clone();
        newParameters.setParameterValue(TICVisualizerParameters.msLevel,
                msLevel);
        newParameters.setParameterValue(TICVisualizerParameters.plotType,
                plotType);
        newParameters.setParameterValue(
                TICVisualizerParameters.retentionTimeRange, rtRange);
        newParameters.setParameterValue(TICVisualizerParameters.mzRange,
                mzRange);
        showNewTICVisualizerWindow(dataFiles, peaks, newParameters);
    }

    public void showNewTICVisualizerWindow(RawDataFile[] dataFiles,
            ChromatographicPeak[] peaks) {
        showNewTICVisualizerWindow(dataFiles, peaks, parameters);
    }

    private void showNewTICVisualizerWindow(RawDataFile[] dataFiles,
            ChromatographicPeak[] peaks, TICVisualizerParameters parameters) {

        logger.finest("Opening a new TIC visualizer setup dialog");

        if ((dataFiles == null) || (dataFiles.length == 0)) {
            desktop.displayErrorMessage("Please select at least one data file");
            return;
        }

        Hashtable<Parameter, Object> autoValues = null;
        if (dataFiles.length == 1) {
            autoValues = new Hashtable<Parameter, Object>();
            autoValues.put(TICVisualizerParameters.msLevel, 1);
            autoValues.put(TICVisualizerParameters.retentionTimeRange,
                    dataFiles[0].getDataRTRange(1));
            autoValues.put(TICVisualizerParameters.mzRange,
                    dataFiles[0].getDataMZRange(1));

        }

        ParameterSetupDialog dialog = new ParameterSetupDialog(
                "Please set parameter values for " + toString(), parameters,
                autoValues);

        dialog.setVisible(true);

        if (dialog.getExitCode() != ExitCode.OK)
            return;

        int msLevel = (Integer) parameters.getParameterValue(TICVisualizerParameters.msLevel);

        Range rtRange = (Range) parameters.getParameterValue(TICVisualizerParameters.retentionTimeRange);
        Range mzRange = (Range) parameters.getParameterValue(TICVisualizerParameters.mzRange);

        this.parameters = parameters;

        Object plotType = parameters.getParameterValue(TICVisualizerParameters.plotType);

        TICVisualizerWindow newWindow = new TICVisualizerWindow(dataFiles,
                plotType, msLevel, rtRange, mzRange, peaks);

        desktop.addInternalFrame(newWindow);

    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#toString()
     */
    public String toString() {
        return "TIC/XIC visualizer";
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