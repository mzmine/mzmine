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

package net.sf.mzmine.methods.gapfilling.simple;

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
import net.sf.mzmine.methods.MethodListener;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

// TODO: Code for this method must be rewritten

public class SimpleGapFiller implements Method,
ListSelectionListener, ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private SimpleGapFillerParameters parameters;
	
	private MethodListener afterMethodListener;
	private int taskCount;
	
    private TaskController taskController;
    private Desktop desktop;
    private JMenuItem myMenuItem;
	
	
	public boolean askParameters() {

		parameters = new SimpleGapFillerParameters();
		
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
		this.parameters = (SimpleGapFillerParameters)parameters;
	}
	
	public String toString() {
		return "Simple Gap filler";
	}

    /**
     * @see net.sf.mzmine.methods.Method#runMethod(net.sf.mzmine.data.impl.SimpleParameterSet, net.sf.mzmine.io.OpenedRawDataFile[], net.sf.mzmine.data.AlignmentResult[])
     */
    public void runMethod(OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults) {

    	if (parameters==null) parameters = new SimpleGapFillerParameters();
    	
        logger.info("Running " + toString() + " on " + alignmentResults.length + " alignment results.");

        SimpleGapFillerMain fillerMain = new SimpleGapFillerMain(taskController, alignmentResults[0], (SimpleGapFillerParameters) parameters);
        fillerMain.doTasks(afterMethodListener);
        
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
        
        myMenuItem = desktop.addMenuItem(MZmineMenu.ALIGNMENT,
                "Simple gap filler", this, null, KeyEvent.VK_S,
                false, false);

        desktop.addSelectionListener(this);
        
        
        
    }

	public void valueChanged(ListSelectionEvent e) {
        AlignmentResult[] alignmentResults = desktop.getSelectedAlignmentResults();
        if ( (alignmentResults==null) || (alignmentResults.length==0) ) myMenuItem.setEnabled(false); else myMenuItem.setEnabled(true); 

		
	}

	public void actionPerformed(ActionEvent e) {
        if (!askParameters()) return;
        
        AlignmentResult[] alignmentResults = desktop.getSelectedAlignmentResults();      

        runMethod(null, alignmentResults);
		
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