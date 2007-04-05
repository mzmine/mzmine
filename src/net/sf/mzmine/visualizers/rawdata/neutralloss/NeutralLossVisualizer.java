/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.visualizers.rawdata.neutralloss;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.util.CollectionUtils;

/**
 * Neutral loss (MS/MS) visualizer using JFreeChart library
 */
public class NeutralLossVisualizer implements MZmineModule, ActionListener,
        ListSelectionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private TaskController taskController;
    private Desktop desktop;

    private JMenuItem nlMenuItem;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {

        this.taskController = core.getTaskController();
        this.desktop = core.getDesktop();

        nlMenuItem = desktop.addMenuItem(MZmineMenu.VISUALIZATION,
                "Neutral loss", this, null, KeyEvent.VK_N, false, false);
        desktop.addSelectionListener(this);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        logger.finest("Opening a new neutral loss visualizer setup dialog");

        OpenedRawDataFile dataFiles[] = desktop.getSelectedDataFiles();

        for (OpenedRawDataFile dataFile : dataFiles) {
            int msLevels[] = dataFile.getCurrentFile().getMSLevels();
            final int neededMSLevels[] = { 1, 2 };
            
            if (! CollectionUtils.isSubset(msLevels, neededMSLevels)) {
                desktop.displayErrorMessage("File " + dataFile
                        + " does not contain data for MS levels 1 and 2.");
                continue;
            }
            JDialog setupDialog = new NeutralLossSetupDialog(taskController,
                    desktop, dataFile);
            setupDialog.setVisible(true);
        }

    }

    /**
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {
        nlMenuItem.setEnabled(desktop.isDataFileSelected());
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#toString()
     */
    public String toString() {
        return "Neutral loss visualizer";
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getCurrentParameters()
     */
    public ParameterSet getCurrentParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setCurrentParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setCurrentParameters(ParameterSet parameterValues) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameterValues) {
        // TODO Auto-generated method stub
        
    }

}