/*
    Copyright 2005-2006 VTT Biotechnology

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

package net.sf.mzmine.methods.alignment.filterbygaps;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.batchmode.BatchModeController.BatchModeStep;
import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodListener;
import net.sf.mzmine.methods.MethodListener.MethodReturnStatus;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;


/**
 * This class implements a filter for alignment results
 * Filter removes rows which have less than defined number of peaks detected
 *
 */
public class AlignmentResultFilterByGaps implements Method,
TaskListener, ListSelectionListener, ActionListener { 

	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private AlignmentResultFilterByGapsParameters parameters;
	
	private MethodListener afterMethodListener;
	private int taskCount;
	
    private TaskController taskController;
    private Desktop desktop;
    private JMenuItem myMenuItem;	

	public String toString() {
		return new String("Filter alignment result rows by gaps");
	}	

	public boolean askParameters() {

        parameters = new AlignmentResultFilterByGapsParameters();

        ParameterSetupDialog dialog = new ParameterSetupDialog(		
        				MainWindow.getInstance(),
        				"Please check parameter values for " + toString(),
        				parameters
        		);
        dialog.setVisible(true);
        
		if (dialog.getExitCode()==-1) return false;

		return true;
        
	}
	
	public void setParameters(SimpleParameterSet parameters) {
		this.parameters = (AlignmentResultFilterByGapsParameters)parameters;
	}




    /**
     * @see net.sf.mzmine.methods.Method#runMethod(net.sf.mzmine.data.impl.SimpleParameterSet, net.sf.mzmine.io.OpenedRawDataFile[], net.sf.mzmine.data.AlignmentResult[])
     */
    public void runMethod(OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults) {
        logger.info("Running " + toString() + " on " + alignmentResults.length + " alignment results.");
        
        if (parameters==null) parameters = new AlignmentResultFilterByGapsParameters();

        taskCount = alignmentResults.length;
        for (AlignmentResult alignmentResult : alignmentResults) {
    		Task alignmentTask = new AlignmentResultFilterByGapsTask(alignmentResult, (AlignmentResultFilterByGapsParameters) parameters);
    		taskController.addTask(alignmentTask, this);
        }
        
    }
    
    public void runMethod(OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults, MethodListener methodListener) {
    	this.afterMethodListener = methodListener;
    	runMethod(dataFiles, alignmentResults);
    }    

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {
    	
        this.taskController = core.getTaskController();
        this.desktop = core.getDesktop();
        
        desktop.addMenuSeparator(MZmineMenu.ALIGNMENT);
        
        myMenuItem = desktop.addMenuItem(MZmineMenu.ALIGNMENT,
                toString(), this, null, KeyEvent.VK_A,
                false, false);

        desktop.addSelectionListener(this);
     
        core.getBatchModeController().registerMethod(BatchModeStep.ALIGNMENTPROCESSING, this);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        if (!askParameters()) return;
        
        AlignmentResult[] alignmentResults = desktop.getSelectedAlignmentResults();      

        runMethod(null, alignmentResults);

    }    

    /**
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {
        //myMenuItem.setEnabled(desktop.isDataFileSelected());

        AlignmentResult[] alignmentResults = desktop.getSelectedAlignmentResults();
        if ( (alignmentResults==null) || (alignmentResults.length==0) ) myMenuItem.setEnabled(false); else myMenuItem.setEnabled(true); 

    }
    
    
    public void taskStarted(Task task) {
        // do nothing
    }

    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

			Object[] results = (Object[]) task.getResult();
			AlignmentResult originalAlignmentResult = (AlignmentResult)results[0];
			AlignmentResult filteredAlignmentResult = (AlignmentResult)results[1];
			AlignmentResultFilterByGapsParameters parameters = (AlignmentResultFilterByGapsParameters)results[2];
			
			// TODO: Add method and parameters to history of an alignment result
			
			MZmineProject.getCurrentProject().addAlignmentResult(filteredAlignmentResult);
			
			taskCount--;
			if ((taskCount==0) && (afterMethodListener!=null)) {
				afterMethodListener.methodFinished(MethodReturnStatus.FINISHED);
				afterMethodListener = null;
			}

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            String msg = "Error while filtering alignment result(s): "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);

			taskCount=0;
			if (afterMethodListener!=null) {
				afterMethodListener.methodFinished(MethodReturnStatus.ERROR);
				afterMethodListener = null;
			}
          
            
        } else if (task.getStatus() == Task.TaskStatus.CANCELED) {
			taskCount=0;
			if (afterMethodListener!=null) {
				afterMethodListener.methodFinished(MethodReturnStatus.CANCELED);
				afterMethodListener = null;
			}
        	
        }

        
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
    
    
}