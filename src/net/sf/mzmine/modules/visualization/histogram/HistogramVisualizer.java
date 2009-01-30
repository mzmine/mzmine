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

package net.sf.mzmine.modules.visualization.histogram;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.main.mzmineclient.MZmineModule;
import net.sf.mzmine.util.dialogs.ExitCode;

public class HistogramVisualizer implements MZmineModule, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    private HistogramParameters parameters;

    private Desktop desktop;

    /**
     * @see net.sf.mzmine.main.mzmineclient.MZmineModule#initModule(net.sf.mzmine.main.mzmineclient.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        this.parameters = new HistogramParameters();

        desktop.addMenuItem(MZmineMenu.VISUALIZATIONPEAKLIST, "Histogram",
                "Visualization of peak list data in a histogram", KeyEvent.VK_H, false,
                this, null);

    }
    
   
    /**
     * 
     */
    public ExitCode setupParameters(ParameterSet parameters, PeakList peakList) {
    	HistogramSetupDialog dialog = new HistogramSetupDialog(
                "Please set parameter values for " + toString(),
                (SimpleParameterSet) parameters, peakList);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        logger.finest("Opening histogram window");

        PeakList[] peaklists = desktop.getSelectedPeakLists();

        if (peaklists.length != 1) {
            desktop.displayErrorMessage("Please select a peak list for visualization in histogram plot");
            return;
        }
        

        for (PeakList pl : peaklists) {
        	
            parameters.setMultipleSelection(HistogramParameters.dataFiles, pl.getRawDataFiles());
        	
            ExitCode exitCode = setupParameters(parameters, pl);
            if (exitCode != ExitCode.OK) {
                return;
            }

            HistogramWindow newWindow = new HistogramWindow(peaklists[0], parameters);

            desktop.addInternalFrame(newWindow);
        }
        
        
    }
    

    /**
     * @see net.sf.mzmine.main.mzmineclient.MZmineModule#toString()
     */
    public String toString() {
        return "Histogram plot";
    }

	public ParameterSet getParameterSet() {
		return parameters;
	}

	public void setParameters(ParameterSet parameterValues) {
		this.parameters = (HistogramParameters) parameterValues;
	}


}