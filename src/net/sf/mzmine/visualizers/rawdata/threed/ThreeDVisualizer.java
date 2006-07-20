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

package net.sf.mzmine.visualizers.rawdata.threed;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.io.IOController;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;

/**
 * 3D visualizer using VisAD library
 */
public class ThreeDVisualizer implements MZmineModule, ListSelectionListener,
        ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private TaskController taskController;
    private Desktop desktop;

    private JMenuItem threeDMenuItem;


    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {
        
        this.taskController = core.getTaskController();
        this.desktop = core.getDesktop();

        threeDMenuItem = desktop.addMenuItem(MZmineMenu.VISUALIZATION,
                "3D plot", this, null, KeyEvent.VK_3, false, false);
        desktop.addSelectionListener(this);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        logger.finest("Opening a new 3D visualizer setup dialog");

        OpenedRawDataFile dataFiles[] = desktop.getSelectedDataFiles();

        for (OpenedRawDataFile dataFile : dataFiles) {
            JDialog setupDialog = new ThreeDSetupDialog(taskController,
                    desktop, dataFile);
            setupDialog.setVisible(true);
        }

    }

    /**
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {
        threeDMenuItem.setEnabled(desktop.isDataFileSelected());
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getModuleDescription()
     */
    public String getModuleDescription() {
        return "3D visualizer";
    }

}