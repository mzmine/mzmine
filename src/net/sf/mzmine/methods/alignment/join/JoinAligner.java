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

package net.sf.mzmine.methods.alignment.join;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.PeakList;
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



/**
 *
 */
public class JoinAligner implements Method,
        TaskListener, ListSelectionListener, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private JoinAlignerParameters parameters;
    
    private TaskController taskController;
    private Desktop desktop;
    private JMenuItem myMenuItem;
    
    


	public String toString() {
		return new String("Join Aligner");
	}



    /**
     * @see net.sf.mzmine.methods.Method#askParameters()
     */
    public boolean askParameters() {

        parameters = new JoinAlignerParameters();

        ParameterSetupDialog dialog = new ParameterSetupDialog(		
        				MainWindow.getInstance(),
        				"Please check parameter values for " + toString(),
        				parameters
        		);
        dialog.setVisible(true);
        
		if (dialog.getExitCode()==-1) return false;

		return true;

    }

    /**
     * @see net.sf.mzmine.methods.Method#runMethod(net.sf.mzmine.methods.MethodParameters, net.sf.mzmine.io.OpenedRawDataFile[], net.sf.mzmine.methods.alignment.AlignmentResult[])
     */
    public void runMethod(MethodParameters parameters, OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults) {

        logger.info("Running " + toString() + " on " + dataFiles.length + " peak lists.");

		Task alignmentTask = new JoinAlignerTask(dataFiles, (JoinAlignerParameters) parameters);
		taskController.addTask(alignmentTask, this);

    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {

        this.taskController = core.getTaskController();
        this.desktop = core.getDesktop();
        
        myMenuItem = desktop.addMenuItem(MZmineMenu.ALIGNMENT,
                "Peak list aligner", this, null, KeyEvent.VK_A,
                false, false);

        desktop.addSelectionListener(this);


    }


    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        if (!askParameters()) return;

        OpenedRawDataFile[] dataFiles = desktop.getSelectedDataFiles();      

        runMethod(parameters, dataFiles, null);

    }

    /**
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {
        //myMenuItem.setEnabled(desktop.isDataFileSelected());

        OpenedRawDataFile[] dataFiles = desktop.getSelectedDataFiles();

		boolean allOk = true;

        for (OpenedRawDataFile file : dataFiles) {
			if (!file.getCurrentFile().hasData(PeakList.class)) {
				allOk = false;
            }
        }
        myMenuItem.setEnabled(allOk);

    }



    public void taskStarted(Task task) {
        // do nothing
    }

    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

			Object[] results = (Object[]) task.getResult();
			AlignmentResult alignmentResult = (AlignmentResult)results[0];
			JoinAlignerParameters parameters = (JoinAlignerParameters)results[1];

			MZmineProject.getCurrentProject().addAlignmentResult(alignmentResult);


        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            String msg = "Error while aligning peak lists: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);
        }

	}

}
