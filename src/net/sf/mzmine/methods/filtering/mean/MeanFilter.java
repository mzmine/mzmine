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

package net.sf.mzmine.methods.filtering.mean;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodListener;
import net.sf.mzmine.methods.MethodListener.MethodReturnStatus;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

/**
 * This class represent a method for filtering scans in raw data file with
 * moving average filter.
 */
public class MeanFilter implements Method, TaskListener,
        ListSelectionListener, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    protected static final Parameter oneSidedWindowLength = new SimpleParameter(        
            ParameterType.DOUBLE,
            "Window length",
            "One-sided length of the smoothing window",
            "Da",
            new Double(0.1),
            new Double(0.0),
            null
            ); 
    
    
    private SimpleParameterSet parameters;
    
    private MethodListener afterMethodListener;
    private int taskCount;
    
    private TaskController taskController;
    private Desktop desktop;
    private JMenuItem myMenuItem;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {

        this.taskController = core.getTaskController();
        this.desktop = core.getDesktop();

        myMenuItem = desktop.addMenuItem(MZmineMenu.FILTERING,
                "Mean filter spectra", this, null, KeyEvent.VK_M, false, false);

        desktop.addSelectionListener(this);
        

    }

    /**
     * This function displays a modal dialog to define method parameters
     * 
     * @see net.sf.mzmine.methods.Method#askParameters()
     */
    public boolean askParameters() {

        //parameters = new MeanFilterParameters();  	

        ParameterSetupDialog dialog = new ParameterSetupDialog(		
        				MainWindow.getInstance(),
        				"Please check parameter values for " + toString(),
        				parameters
        		);
        dialog.setVisible(true);
        
//		if (dialog.getExitCode()==-1) return false;

		return true;
    }

    public void setParameters(SimpleParameterSet parameters) {
   // 	this.parameters = (MeanFilterParameters)parameters;
    }
    
    /**
     * @see net.sf.mzmine.methods.Method#runMethod(net.sf.mzmine.data.impl.SimpleParameterSet,
     *      net.sf.mzmine.io.RawDataFile[],
     *      net.sf.mzmine.methods.alignment.AlignmentResult[])
     */
    public void runMethod(OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults) {

    	//if (parameters==null) parameters = new MeanFilterParameters();
    	
        logger.info("Running " + toString() + " on " + dataFiles.length + " raw data files.");

        taskCount = dataFiles.length;
        for (OpenedRawDataFile dataFile : dataFiles) {
         //   Task filterTask = new MeanFilterTask(dataFile,
             //        parameters);
          //  taskController.addTask(filterTask, this);
        }

    }
    
    public void runMethod(OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults, MethodListener methodListener) {
    	this.afterMethodListener = methodListener;
    	runMethod(dataFiles, alignmentResults);
    }    
    
        

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        if (!askParameters()) return;

        OpenedRawDataFile[] dataFiles = desktop.getSelectedDataFiles();
               
        runMethod(dataFiles, null);

    }

    /**
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {
        myMenuItem.setEnabled(desktop.isDataFileSelected());
    }

    public void taskStarted(Task task) {
        // do nothing
    }

    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

            Object[] result = (Object[]) task.getResult();
            OpenedRawDataFile openedFile = (OpenedRawDataFile) result[0];
            RawDataFile newFile = (RawDataFile) result[1];
            SimpleParameterSet cfParam = (SimpleParameterSet) result[2];

            openedFile.updateFile(newFile, this, cfParam);
            
            taskCount--;
            if ((taskCount==0) && (afterMethodListener!=null)) {
            	afterMethodListener.methodFinished(MethodReturnStatus.FINISHED);
            	afterMethodListener = null;
            }

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            String msg = "Error while filtering a file: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);
            
            taskCount = 0;
            if (afterMethodListener!=null) {
            	afterMethodListener.methodFinished(MethodReturnStatus.ERROR);
            	afterMethodListener = null;
            }
            
        } else if (task.getStatus() == Task.TaskStatus.CANCELED) {
            taskCount = 0;
            if (afterMethodListener!=null) {
            	afterMethodListener.methodFinished(MethodReturnStatus.CANCELED);
            	afterMethodListener = null;
            }        	
        }
        
    }

    /**
     * @see net.sf.mzmine.methods.Method#toString()
     */
    public String toString() {
        return "Moving average filter";
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
     * @see net.sf.mzmine.methods.Method#runMethod(net.sf.mzmine.io.OpenedRawDataFile[], net.sf.mzmine.data.AlignmentResult[], net.sf.mzmine.data.ParameterSet, net.sf.mzmine.methods.MethodListener)
     */
    public void runMethod(OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults, ParameterSet parameters, MethodListener methodListener) {
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
