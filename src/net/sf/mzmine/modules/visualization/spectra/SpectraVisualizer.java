/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.spectra;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * Spectrum visualizer using JFreeChart library
 */
public class SpectraVisualizer implements MZmineModule, ActionListener {

    private SpectraVisualizerParameters parameters;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        parameters = new SpectraVisualizerParameters();

        MZmineCore.getDesktop().addMenuItem(MZmineMenu.VISUALIZATIONRAWDATA,
                "Spectra plot", "Shows an individual spectrum", KeyEvent.VK_S,
                false, this, null);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        logger.finest("Opening a new spectra visualizer setup dialog");

        RawDataFile dataFiles[] = MZmineCore.getDesktop().getSelectedDataFiles();
        if (dataFiles.length != 1) {
            MZmineCore.getDesktop().displayErrorMessage(
                    "Please select a single data file");
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

    public static void showNewSpectrumWindow(RawDataFile dataFile,
            int scanNumber) {
        SpectraVisualizerWindow newWindow = new SpectraVisualizerWindow(
                dataFile, dataFile.toString(), dataFile.getScan(scanNumber));
        MZmineCore.getDesktop().addInternalFrame(newWindow);
    }
    
    public static void showNewSpectrumWindow(Scan scan) {
        showNewSpectrumWindow(scan.getDataFile(), scan.getScanNumber());
    }

    public static void showIsotopePattern(RawDataFile dataFile,
            IsotopePattern isotopePattern) {
        String title = dataFile.toString();
        SpectraVisualizerWindow newWindow = new SpectraVisualizerWindow(
                dataFile, title, isotopePattern);
        MZmineCore.getDesktop().addInternalFrame(newWindow);

    }

    public static void showIsotopePattern(IsotopePattern isotopePattern) {
        SpectraVisualizerWindow newWindow = new SpectraVisualizerWindow(null,
                "", isotopePattern);
        MZmineCore.getDesktop().addInternalFrame(newWindow);
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