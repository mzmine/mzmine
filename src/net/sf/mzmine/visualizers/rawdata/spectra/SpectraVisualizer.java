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

package net.sf.mzmine.visualizers.rawdata.spectra;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.io.IOController;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.visualizers.rawdata.tic.TICSetupDialog;

/**
 * Spectrum visualizer using JFreeChart library
 */
public class SpectraVisualizer implements MZmineModule, ListSelectionListener,
        ActionListener {

    private TaskController taskController;
    private Desktop desktop;
    private Logger logger;

    private JMenuItem spectraMenuItem;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.io.IOController,
     *      net.sf.mzmine.taskcontrol.TaskController,
     *      net.sf.mzmine.userinterface.Desktop, java.util.logging.Logger)
     */
    public void initModule(IOController ioController,
            TaskController taskController, Desktop desktop, Logger logger) {

        this.taskController = taskController;
        this.desktop = desktop;
        this.logger = logger;

        spectraMenuItem = desktop.addMenuItem(MZmineMenu.VISUALIZATION,
                "Spectra plot", this, null, KeyEvent.VK_S, false, false);
        desktop.addSelectionListener(this);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        RawDataFile firstSelectedFile = desktop.getFirstSelectedRawData();
        if (firstSelectedFile == null)
            return;

        logger.finest("Opening a new spectra visualizer setup dialog");
        JDialog setupDialog = new SpectraSetupDialog(taskController, desktop,
                firstSelectedFile);
        setupDialog.setVisible(true);

    }

    /**
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {
        spectraMenuItem.setEnabled(desktop.isRawDataSelected());
    }

}