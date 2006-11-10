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

package net.sf.mzmine.batchmode.impl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.batchmode.BatchModeController;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

/**
 * Batch mode module
 */
public class BatchMode implements BatchModeController, ListSelectionListener,
        ActionListener {

	
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private MZmineCore core;
    private Desktop desktop;

    private JMenuItem batchMenuItem;
    
    private Hashtable<BatchModeStep, ArrayList<Method>> registeredMethods;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {

        this.core = core;
        this.desktop = core.getDesktop();

        batchMenuItem = desktop.addMenuItem(MZmineMenu.BATCH,
                "Define batch operations", this, null, KeyEvent.VK_D, false,
                false);
        desktop.addSelectionListener(this);
        
        registeredMethods = new Hashtable<BatchModeStep, ArrayList<Method>>();

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        logger.finest("Opening a new batch setup dialog");

        OpenedRawDataFile dataFiles[] = desktop.getSelectedDataFiles();

        JDialog setupDialog = new BatchModeDialog(MainWindow.getInstance(), registeredMethods);
        setupDialog.setVisible(true);

    }

    /**
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {
    	batchMenuItem.setEnabled(true);
    	// TODO: Remove above DEBUG code, uncomment row below
        //batchMenuItem.setEnabled(desktop.isDataFileSelected());
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#toString()
     */
    public String toString() {
        return "Batch mode";
    }
    
    public void registerMethod(BatchModeStep batchModeStep, Method method) {
    	ArrayList<Method> methodsForStep = registeredMethods.get(batchModeStep);
    	if (methodsForStep==null) {
    		methodsForStep = new ArrayList<Method>();
    		registeredMethods.put(batchModeStep, methodsForStep);
    	}
    	
    	methodsForStep.add(method);
    	
    }

}