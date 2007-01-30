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
import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.ParameterValue;
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
        ActionListener {

	
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
    
    private OpenedRawDataFile[] selectedDataFiles;
    private AlignmentResult selectedAlignmentResult;
    

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
        
        selectedDataFiles = desktop.getSelectedDataFiles();
        
        if (selectedDataFiles.length!=0) {
            currentBatchStep = BatchStep.values()[0];
            currentBatchStepIndex = 0;
            processStep();
        } else {
            currentBatchStep = BatchStep.values()[0];
            currentBatchStepIndex = 0;        	
        }
        
     


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
    
    public void methodFinished(MethodReturnStatus status) {
    	
    	logger.info("Batch mode received taskFinished call");
    	
		// TODO Auto-generated method stub
    	if (	(status == MethodReturnStatus.ERROR) &&
    			(status == MethodReturnStatus.CANCELED) ) {
    		currentBatchStep = BatchStep.Halt;
    		currentBatchStepIndex = batchStepHaltIndex;
    		return;
    	}

    	if (status == MethodReturnStatus.FINISHED) {

        	switch (currentBatchStep) {
        	case RawDataFilter1:
        	case RawDataFilter2:
        	case RawDataFilter3:
        	case PeakPicker:
        	case PeakListProcessor1:
        	case PeakListProcessor2:
        	case PeakListProcessor3:        		
        		break;
        		
        	case Aligner:
        	case AlignmentResultProcessor1:
        	case AlignmentResultProcessor2:
        	case AlignmentResultProcessor3:        		
        		// Pickup last added alignment result from the list
        		MZmineProject p = MZmineProject.getCurrentProject();
        		AlignmentResult[] aRess = p.getAlignmentResults();
        		selectedAlignmentResult =  aRess[aRess.length-1];
        		break;
        	}
    		
        	advanceToNextStep();
    		processStep();
    	}
		
	}
    

    private void advanceToNextStep() {
		currentBatchStepIndex++;
		currentBatchStep = BatchStep.values()[currentBatchStepIndex];    	
    }
    
    private void processStep() {
    	
    	logger.info("Batch mode processStep");
    	
    	// Pickup method for the step
    	Method m;
    	ParameterValue v;
    	MZmineProject project = MZmineProject.getCurrentProject();
    	switch (currentBatchStep) {
    	case RawDataFilter1:
    		v = project.getParameterValue(BatchModeParameters.methodRawDataFilter1);
    		if (v!=null) {
    			m = (Method)v.getValue();
    			m.runMethod(selectedDataFiles, null, this);
    		} else {
    			advanceToNextStep(); processStep();
    		}
    		break;
    	case RawDataFilter2:
    		v = project.getParameterValue(BatchModeParameters.methodRawDataFilter2);
    		if (v!=null) {
    			m = (Method)v.getValue();
    			m.runMethod(selectedDataFiles, null, this);
    		} else {
    			advanceToNextStep(); processStep();
    		}    		
    		break;
    	case RawDataFilter3:
    		v = project.getParameterValue(BatchModeParameters.methodRawDataFilter3);
    		if (v!=null) {
    			m = (Method)v.getValue();
    			m.runMethod(selectedDataFiles, null, this);
    		} else {
    			advanceToNextStep(); processStep();
    		}
    		break;
    	case PeakPicker:
    		v = project.getParameterValue(BatchModeParameters.methodPeakPicker);
    		if (v!=null) {
    			m = (Method)v.getValue();
    			m.runMethod(selectedDataFiles, null, this);
    		} else {
    			advanceToNextStep(); processStep();
    		}    		
    		break;
    	case PeakListProcessor1:
    		v = project.getParameterValue(BatchModeParameters.methodPeakListProcessor1);
    		if (v!=null) {
    			m = (Method)v.getValue();
    			m.runMethod(selectedDataFiles, null, this);
    		} else {
    			advanceToNextStep(); processStep();
    		}    		  		
    		break;
    	case PeakListProcessor2:
    		v = project.getParameterValue(BatchModeParameters.methodPeakListProcessor2);
    		if (v!=null) {
    			m = (Method)v.getValue();
    			m.runMethod(selectedDataFiles, null, this);
    		} else {
    			advanceToNextStep(); processStep();
    		}
    		break;
    	case PeakListProcessor3:
    		v = project.getParameterValue(BatchModeParameters.methodPeakListProcessor3);
    		if (v!=null) {
    			m = (Method)v.getValue();
    			m.runMethod(selectedDataFiles, null, this);
    		} else {
    			advanceToNextStep(); processStep();
    		}
    		break;
    	case Aligner:
    		v = project.getParameterValue(BatchModeParameters.methodAligner);
    		if (v!=null) {
    			m = (Method)v.getValue();
    			m.runMethod(selectedDataFiles, null, this);
    		} else {
    			advanceToNextStep(); processStep();
    		}
    		break;
    	case AlignmentResultProcessor1:
    		v = project.getParameterValue(BatchModeParameters.methodAlignmentProcessor1);
    		if (v!=null) {
    			m = (Method)v.getValue();
    			AlignmentResult[] aRess = new AlignmentResult[1];
    			aRess[0] = selectedAlignmentResult;
    			m.runMethod(null, aRess, this);
    		} else {
    			advanceToNextStep(); processStep();
    		}	
    		break;
    	case AlignmentResultProcessor2:
    		v = project.getParameterValue(BatchModeParameters.methodAlignmentProcessor2);
    		if (v!=null) {
    			m = (Method)v.getValue();
    			AlignmentResult[] aRess = new AlignmentResult[1];
    			aRess[0] = selectedAlignmentResult;
    			m.runMethod(null, aRess, this);
    		} else {
    			advanceToNextStep(); processStep();
    		}
    		break;
    	case AlignmentResultProcessor3:
    		v = project.getParameterValue(BatchModeParameters.methodAlignmentProcessor3);
    		if (v!=null) {
    			m = (Method)v.getValue();
    			AlignmentResult[] aRess = new AlignmentResult[1];
    			aRess[0] = selectedAlignmentResult;
    			m.runMethod(null, aRess, this);
    		} else {
    			advanceToNextStep(); processStep();
    		}
    		break;    		
    	case Halt:
    		logger.info("Batch processing done.");
    		break;
    	}
    	
    	
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