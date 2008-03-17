/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.modules.dataanalysis.intensityplot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.Desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * Peak intensity plot module
 */
public class IntensityPlot implements MZmineModule, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private Desktop desktop;
    private IntensityPlotParameters parameters;
    private static IntensityPlot myInstance;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        parameters = new IntensityPlotParameters();
        
        myInstance = this;

        desktop.addMenuItem(MZmineMenu.ANALYSIS, "Peak intensity plot", this,
                null, KeyEvent.VK_D, false, true);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        PeakList selectedAlignedPeakLists[] = desktop.getSelectedPeakLists();
        if (selectedAlignedPeakLists.length != 1) {
            desktop.displayErrorMessage("Please select a single aligned peaklist");
            return;
        }

        if (selectedAlignedPeakLists[0].getNumberOfRows() == 0) {
            desktop.displayErrorMessage("Selected alignment result is empty");
            return;
        }

        logger.finest("Showing intensity plot setup dialog");

        if (selectedAlignedPeakLists[0] != parameters.getSourcePeakList()) {
            parameters = new IntensityPlotParameters(
                    selectedAlignedPeakLists[0]);
        }

        IntensityPlotDialog setupDialog = new IntensityPlotDialog(
                selectedAlignedPeakLists[0], parameters);
        setupDialog.setVisible(true);

        if (setupDialog.getExitCode() == ExitCode.OK) {
            logger.info("Opening new intensity plot");
            IntensityPlotFrame newFrame = new IntensityPlotFrame(parameters);
            desktop.addInternalFrame(newFrame);
        }

    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#toString()
     */
    public String toString() {
        return "Peak intensity plot";
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public IntensityPlotParameters getParameterSet() {
        return parameters;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameterValues) {
        this.parameters = (IntensityPlotParameters) parameterValues;
    }
    
    public static IntensityPlot getInstance() {
        return myInstance;
    }

}