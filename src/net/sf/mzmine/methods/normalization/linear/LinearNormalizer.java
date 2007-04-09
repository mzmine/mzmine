/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/


package net.sf.mzmine.methods.normalization.linear;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.TaskSequenceListener;
import net.sf.mzmine.taskcontrol.TaskSequence.TaskSequenceStatus;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;


 // TODO: Code for this method needs to be reorganized

/**
 *
 */
public class LinearNormalizer implements Method,
						TaskListener, ListSelectionListener, ActionListener {

	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private LinearNormalizerParameters parameters;
	
	private TaskSequenceListener afterMethodListener;
	private int taskCount;
	
	private TaskController taskController;
    private Desktop desktop;
    private JMenuItem myMenuItem;
	

    /**
     * @see net.sf.mzmine.methods.Method#askParameters()
     */
    public boolean askParameters() {

        parameters = new LinearNormalizerParameters();

        ParameterSetupDialog dialog = new ParameterSetupDialog(		
        				MainWindow.getInstance(),
        				"Please check parameter values for " + toString(),
        				parameters
        		);
        dialog.setVisible(true);
        
		//if (dialog.getExitCode()==-1) return false;

		return true;

    }
    
    public void setParameters(SimpleParameterSet parameters) {
    	this.parameters = (LinearNormalizerParameters)parameters;
    }

    /**
     * @see net.sf.mzmine.methods.Method#runMethod(net.sf.mzmine.data.impl.SimpleParameterSet, net.sf.mzmine.io.OpenedRawDataFile[], net.sf.mzmine.methods.alignment.AlignmentResult[])
     */
    public void runMethod(OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults) {

    	if (parameters==null) parameters = new LinearNormalizerParameters();
    	
        logger.info("Running " + toString() + " on " + alignmentResults[0].toString());

        taskCount = 1;
		Task alignmentTask = new LinearNormalizerTask(alignmentResults[0], (LinearNormalizerParameters) parameters);
		taskController.addTask(alignmentTask, this);

    }
    
    public void runMethod(OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults, TaskSequenceListener methodListener) {
    	this.afterMethodListener = methodListener;
    	runMethod(dataFiles, alignmentResults);
    }    


    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {
    	
        this.taskController = core.getTaskController();
        this.desktop = core.getDesktop();
        
        myMenuItem = desktop.addMenuItem(MZmineMenu.NORMALIZATION,
                "Linear normalizer", this, null, KeyEvent.VK_A,
                false, false);

        desktop.addSelectionListener(this);

        
    }



	public void actionPerformed(ActionEvent e) {
		
        if (!askParameters()) return;

        AlignmentResult[] alignmentResults = desktop.getSelectedAlignmentResults();      

        runMethod(null, alignmentResults);
		
	}

	public void taskStarted(Task task) {
		// TODO Auto-generated method stub
		
	}

	public void taskFinished(Task task) {
        if (task.getStatus() == Task.TaskStatus.FINISHED) {

			Object[] results = (Object[]) task.getResult();
			AlignmentResult originalAlignmentResult = (AlignmentResult)results[0];
			AlignmentResult normalizedAlignmentResult = (AlignmentResult)results[1];
			LinearNormalizerParameters parameters = (LinearNormalizerParameters)results[2];
			

			// TODO: Add method and parameters to history of an alignment result
			
			MZmineProject.getCurrentProject().addAlignmentResult(normalizedAlignmentResult);
			
			taskCount--;
			if ((taskCount==0) && (afterMethodListener!=null)) {
					//afterMethodListener.taskSequenceFinished(TaskSequenceStatus.FINISHED);
					afterMethodListener=null;
			}            



        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            String msg = "Error while normalizing alignment result: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);
            
			taskCount = 0;
			if (afterMethodListener!=null) {
//					afterMethodListener.taskSequenceFinished(TaskSequenceStatus.ERROR);
					afterMethodListener=null;
			}            
            
        } else if (task.getStatus() == Task.TaskStatus.CANCELED) {
			taskCount = 0;
			if (afterMethodListener!=null) {
//					afterMethodListener.taskSequenceFinished(TaskSequenceStatus.CANCELED);
					afterMethodListener=null;
			}
			
        }     
		
	}


	public void valueChanged(ListSelectionEvent e) {
		
        AlignmentResult[] alignmentResults = desktop.getSelectedAlignmentResults();

        if ( (alignmentResults==null) || (alignmentResults.length!=1) ) {
        	myMenuItem.setEnabled(false);
        } else {
        	myMenuItem.setEnabled(true);
        }
		
	}
	
	public String toString() {
		return "Linear normalizer";
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
     * @see net.sf.mzmine.methods.Method#setupParameters(net.sf.mzmine.data.ParameterSet)
     */
    public ParameterSet setupParameters(ParameterSet current) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see net.sf.mzmine.methods.Method#runMethod(net.sf.mzmine.io.OpenedRawDataFile[], net.sf.mzmine.data.AlignmentResult[], net.sf.mzmine.data.ParameterSet)
     */
    public void runMethod(OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults, ParameterSet parameters) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @see net.sf.mzmine.methods.Method#runMethod(net.sf.mzmine.io.OpenedRawDataFile[], net.sf.mzmine.data.AlignmentResult[], net.sf.mzmine.data.ParameterSet, net.sf.mzmine.taskcontrol.TaskSequenceListener)
     */
    public void runMethod(OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults, ParameterSet parameters, TaskSequenceListener methodListener) {
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
