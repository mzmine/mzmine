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

package net.sf.mzmine.modules.visualization.twod;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Hashtable;
import java.util.logging.Logger;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * 2D visualizer using JFreeChart library
 */
public class TwoDVisualizer implements MZmineModule, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private TwoDParameters parameters;

    private Desktop desktop;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

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

        Hashtable<Parameter, Object> autoValues = new Hashtable<Parameter, Object>();
        autoValues.put(TwoDParameters.msLevel, 1);
        autoValues.put(TwoDParameters.retentionTimeRange,
                dataFiles[0].getDataRTRange(1));
        autoValues.put(TwoDParameters.mzRange, dataFiles[0].getDataMZRange(1));

        ParameterSetupDialog dialog = new ParameterSetupDialog(
                "Please set parameter values for " + toString(), parameters,
                autoValues);

        dialog.setVisible(true);

        if (dialog.getExitCode() != ExitCode.OK)
            return;

        int msLevel = (Integer) parameters.getParameterValue(TwoDParameters.msLevel);
        Range rtRange = (Range) parameters.getParameterValue(TwoDParameters.retentionTimeRange);
        Range mzRange = (Range) parameters.getParameterValue(TwoDParameters.mzRange);

        // Create a window, but do not add it to the desktop. It will be added
        // automatically after finishing the sampling task.
        new TwoDVisualizerWindow(dataFiles[0], msLevel, rtRange, mzRange);

    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#toString()
     */
    public String toString() {
        return "2D visualizer";
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
        this.parameters = (TwoDParameters) parameters;
    }

}