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

package net.sf.mzmine.modules.visualization.spectra;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * Spectrum visualizer using JFreeChart library
 */
public class SpectraVisualizer implements MZmineModule, ActionListener {

    private static SpectraVisualizer myInstance;

    private SpectraVisualizerParameters parameters;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private Desktop desktop;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        myInstance = this;

        parameters = new SpectraVisualizerParameters();

        desktop.addMenuItem(MZmineMenu.VISUALIZATION, "Spectra plot",
                "Shows an individual spectrum", KeyEvent.VK_S, this, null);

    }

    public static SpectraVisualizer getInstance() {
        return myInstance;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        logger.finest("Opening a new spectra visualizer setup dialog");

        RawDataFile dataFiles[] = desktop.getSelectedDataFiles();
        if (dataFiles.length != 1) {
            desktop.displayErrorMessage("Please select a single data file");
            return;
        }

        showNewSpectrumWindow(dataFiles[0], parameters);

    }

    private void showNewSpectrumWindow(RawDataFile dataFile,
            SpectraVisualizerParameters parameters) {

        ParameterSetupDialog dialog = new ParameterSetupDialog(
                "Please set parameter values for " + toString(), parameters);

        dialog.setVisible(true);

        if (dialog.getExitCode() != ExitCode.OK)
            return;

        Integer scanNumber = (Integer) parameters.getParameterValue(SpectraVisualizerParameters.scanNumber);

        showNewSpectrumWindow(dataFile, scanNumber);

    }

    public void showNewSpectrumWindow(RawDataFile dataFile, int scanNumber) {
        SpectraVisualizerWindow newWindow = new SpectraVisualizerWindow(
                dataFile, scanNumber);
        desktop.addInternalFrame(newWindow);
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#toString()
     */
    public String toString() {
        return "Spectra visualizer";
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
        parameters = (SpectraVisualizerParameters) newParameters;
    }

}