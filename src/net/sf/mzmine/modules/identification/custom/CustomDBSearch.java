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

package net.sf.mzmine.modules.identification.custom;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.modules.BatchStep;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ExitCode;

/**
 * 
 */
public class CustomDBSearch implements MZmineModule, BatchStep, ActionListener {

    public static final String MODULE_NAME = "Custom database search";
    
    private MZmineCore core;
    private Desktop desktop;
    
    private CustomDBSearchParameters parameters;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {
        
        this.core = core;
        this.desktop = core.getDesktop();
        
        parameters = new CustomDBSearchParameters();
       
        desktop.addMenuItem(MZmineMenu.IDENTIFICATION, MODULE_NAME, this,
                null, KeyEvent.VK_C, false, true);
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
    public void setParameters(ParameterSet parameterValues) {
        this.parameters = (CustomDBSearchParameters) parameterValues;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        
        PeakList[] selectedPeakLists = desktop.getSelectedAlignedPeakLists();
        if (selectedPeakLists.length < 1) {
            desktop.displayErrorMessage("Please select aligned peak list");
            return;
        }
        
        for (PeakList peakList : selectedPeakLists) {
            CustomDBSearchDialog dialog = new CustomDBSearchDialog(core, peakList);
            dialog.setVisible(true);
        }
        
    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.io.OpenedRawDataFile[], net.sf.mzmine.data.PeakList[], net.sf.mzmine.data.ParameterSet, net.sf.mzmine.taskcontrol.TaskGroupListener)
     */
    public TaskGroup runModule(OpenedRawDataFile[] dataFiles, PeakList[] alignmentResults, ParameterSet parameters, TaskGroupListener methodListener) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#setupParameters(net.sf.mzmine.data.ParameterSet)
     */
    public ExitCode setupParameters(ParameterSet current) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public String toString() {
        return MODULE_NAME;
    }




}
