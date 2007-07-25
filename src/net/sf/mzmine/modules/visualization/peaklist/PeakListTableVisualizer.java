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

package net.sf.mzmine.modules.visualization.peaklist;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JMenuItem;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.util.logging.JCommonLogHandler;

import org.jfree.report.JFreeReportBoot;

public class PeakListTableVisualizer implements MZmineModule, ActionListener {

    private Desktop desktop;
    private PeakListTableParameters parameters;
    private JMenuItem peakListTable, alignedPeakListTable;
    private static PeakListTableVisualizer myInstance;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();
        myInstance = this;
        
        // boot the JFreeReport library and register our logging handler,
        // to get a rid of JCommon debug messages on the console
        JFreeReportBoot.getInstance().start();
        JCommonLogHandler.register();
        
        parameters = new PeakListTableParameters();

        peakListTable = desktop.addMenuItem(MZmineMenu.VISUALIZATION,
                "Peak list table view", this, null, KeyEvent.VK_P, false, true);

        alignedPeakListTable = desktop.addMenuItem(MZmineMenu.VISUALIZATION,
                "Aligned peak list table view", this, null, KeyEvent.VK_A,
                false, true);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        RawDataFile dataFiles[] = desktop.getSelectedDataFiles();
        PeakList alignmentResults[] = desktop.getSelectedAlignedPeakLists();
        MZmineProject currentProject = MZmineCore.getCurrentProject();
        
        Object src = e.getSource();

        if (src == peakListTable) {

                if (dataFiles.length == 0) {
                    desktop.displayErrorMessage("Please select data file");
                    return;
                }

                for (RawDataFile dataFile : dataFiles) {

                    PeakList peakList = currentProject.getFilePeakList(dataFile);
                    
                    if (peakList == null) {
                        desktop.displayErrorMessage(dataFile + " has no peak list, please run peak picking first");
                        return;
                    }
                    
                    logger.finest("Showing new peak list view");
                    PeakListTableWindow alignmentResultView = new PeakListTableWindow(
                            peakList);
                    desktop.addInternalFrame(alignmentResultView);

                }
        }

        if (src == alignedPeakListTable) {

            if (alignmentResults.length == 0) {
                desktop.displayErrorMessage("Please select aligned peak list");
                return;
            }

            for (PeakList alignmentResult : alignmentResults) {

                logger.finest("Showing a new alignment result list view");

                PeakListTableWindow alignmentResultView = new PeakListTableWindow(
                        alignmentResult);
                desktop.addInternalFrame(alignmentResultView);

            }
        }

    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#toString()
     */
    public String toString() {
        return "Alignment result table visualizer";
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public PeakListTableParameters getParameterSet() {
        return parameters;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameterValues) {
        parameters = (PeakListTableParameters) parameterValues;
    }
    
    public static PeakListTableVisualizer getInstance() {
        return myInstance;
    }

}