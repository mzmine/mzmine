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
import java.util.Iterator;
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
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.methods.alignment.join.JoinAlignerParameters;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.Task.TaskStatus;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

/**
 * Batch mode module
 */
public class BatchMode implements BatchModeController, ListSelectionListener,
        ActionListener, TaskListener {

	
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private MZmineCore core;
    private Desktop desktop;

    private JMenuItem batchMenuItem;
    
    private Hashtable<BatchModeStep, ArrayList<Method>> registeredMethods;
    
    private BatchModeParameters parameters;
    
    private enum BatchStep {	 
    								RawDataFilter1, RawDataFilter2, RawDataFilter3,
    								PeakPicker,
    								PeakListProcessor1, PeakListProcessor2, PeakListProcessor3,
    								Aligner,
    								AlignmentResultProcessor1, AlignmentResultProcessor2, AlignmentResultProcessor3,
    								Halt;
    							 };
    private int batchStepHaltIndex = BatchStep.values().length-1;
    							 
    private BatchStep currentBatchStep = BatchStep.Halt;
    private int currentBatchStepIndex = batchStepHaltIndex;
    

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
    	
    	logger.finest("Showing parameter setup dialog");

    	parameters = new BatchModeParameters();
    	JDialog setupDialog = new BatchModeDialog(MainWindow.getInstance(), registeredMethods, parameters);
        setupDialog.setVisible(true);
        
        OpenedRawDataFile dataFiles[] = desktop.getSelectedDataFiles();
     
        currentBatchStep = BatchStep.values()[0];
        currentBatchStepIndex = 0;

    }

    /**
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {
    	batchMenuItem.setEnabled(desktop.isDataFileSelected());
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#toString()
     */
    public String toString() {
        return "Batch mode";
    }
    
    public void taskFinished(Task task) {
		// TODO Auto-generated method stub
    	if (	(task.getStatus() == TaskStatus.ERROR) &&
    			(task.getStatus() == TaskStatus.CANCELED) ) {
    		currentBatchStep = BatchStep.Halt;
    		currentBatchStepIndex = batchStepHaltIndex;
    		return;
    	}

    	if (task.getStatus() == TaskStatus.FINISHED) {
    		
    		currentBatchStepIndex++;
    		currentBatchStep = BatchStep.values()[currentBatchStepIndex];
    		proceedToNextStep();
    	}
		
	}
    
    private void proceedToNextStep() {
    	
    	// Pickup method for the step
    	Method m;
    	MZmineProject project = MZmineProject.getCurrentProject();
    	switch (currentBatchStep) {
    	case RawDataFilter1:
    		m = (Method)project.getParameterValue(BatchModeParameters.methodRawDataFilter1).getValue();
    		break;
    	case RawDataFilter2:
    		m = (Method)project.getParameterValue(BatchModeParameters.methodRawDataFilter2).getValue();
    		break;
    	case RawDataFilter3:
    		m = (Method)project.getParameterValue(BatchModeParameters.methodRawDataFilter3).getValue();
    		break;
    	case PeakPicker:
    		m = (Method)project.getParameterValue(BatchModeParameters.methodPeakPicker).getValue();
    		break;
    	case PeakListProcessor1:
    		m = (Method)project.getParameterValue(BatchModeParameters.methodPeakListProcessor1).getValue();
    		break;
    	case PeakListProcessor2:
    		m = (Method)project.getParameterValue(BatchModeParameters.methodPeakListProcessor2).getValue();
    		break;
    	case PeakListProcessor3:
    		m = (Method)project.getParameterValue(BatchModeParameters.methodPeakListProcessor3).getValue();
    		break;
    	case Aligner:
    		m = (Method)project.getParameterValue(BatchModeParameters.methodAligner).getValue();
    		break;
    	case AlignmentResultProcessor1:
    		m = (Method)project.getParameterValue(BatchModeParameters.methodAlignmentProcessor1).getValue();
    		break;
    	case AlignmentResultProcessor2:
    		m = (Method)project.getParameterValue(BatchModeParameters.methodAlignmentProcessor2).getValue();
    		break;
    	case AlignmentResultProcessor3:
    		m = (Method)project.getParameterValue(BatchModeParameters.methodAlignmentProcessor3).getValue();
    		break;    		
    	case Halt:
    		logger.info("Batch processing done.");
    		break;
    	}
    	
    	
    }

	public void taskStarted(Task task) {
		// TODO Auto-generated method stub
		
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