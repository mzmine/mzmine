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

import net.sf.mzmine.batchmode.BatchModeController.BatchModeStep;
import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
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
	
	private TaskListener additionalTaskListener;
	
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
        
		if (dialog.getExitCode()==-1) return false;

		return true;

    }
    
    public void setParameters(MethodParameters parameters) {
    	this.parameters = (LinearNormalizerParameters)parameters;
    }

    /**
     * @see net.sf.mzmine.methods.Method#runMethod(net.sf.mzmine.methods.MethodParameters, net.sf.mzmine.io.OpenedRawDataFile[], net.sf.mzmine.methods.alignment.AlignmentResult[])
     */
    public void runMethod(OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults) {

    	if (parameters==null) parameters = new LinearNormalizerParameters();
    	
        logger.info("Running " + toString() + " on " + alignmentResults[0].toString());

		Task alignmentTask = new LinearNormalizerTask(alignmentResults[0], (LinearNormalizerParameters) parameters);
		taskController.addTask(alignmentTask, this);

    }
    
    public void runMethod(OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults, TaskListener additionalTaskListener) {
    	this.additionalTaskListener = additionalTaskListener;
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

        core.getBatchModeController().registerMethod(BatchModeStep.ALIGNMENTPROCESSING, this);
        
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


        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            String msg = "Error while normalizing alignment result: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);
        }
        
        if (additionalTaskListener!=null) additionalTaskListener.taskFinished(task);
        additionalTaskListener=null;        
		
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
	
}
