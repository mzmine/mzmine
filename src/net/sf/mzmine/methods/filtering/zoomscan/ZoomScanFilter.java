/*
 * Copyright 2006 The MZmine Development Team This file is part of MZmine.
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. MZmine is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with MZmine; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.methods.filtering.zoomscan;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.io.IOController;
import net.sf.mzmine.io.MZmineOpenedFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodParameters;
import net.sf.mzmine.methods.alignment.AlignmentResult;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;

public class ZoomScanFilter implements MZmineModule, Method, TaskListener,
        ListSelectionListener, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private TaskController taskController;
    private Desktop desktop;
    private JMenuItem myMenuItem;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.io.IOController,
     *      net.sf.mzmine.taskcontrol.TaskController,
     *      net.sf.mzmine.userinterface.Desktop)
     */
    public void initModule(IOController ioController,
            TaskController taskController, Desktop desktop) {

        this.taskController = taskController;
        this.desktop = desktop;

        myMenuItem = desktop.addMenuItem(MZmineMenu.FILTERING,
                "Zoom scan filter", this, null, KeyEvent.VK_Z, false, false);

        desktop.addSelectionListener(this);

    }

    /**
     * This function displays a modal dialog to define method parameters
     * 
     * @see net.sf.mzmine.methods.Method#askParameters()
     */
    public MethodParameters askParameters() {

        MZmineProject currentProject = MZmineProject.getCurrentProject();
        ZoomScanFilterParameters currentParameters = (ZoomScanFilterParameters) currentProject.getParameters(this);
        if (currentParameters == null)
            currentParameters = new ZoomScanFilterParameters();

        // Initialize parameter setup dialog
        double[] paramValues = new double[1];
        paramValues[0] = currentParameters.minMZRange;

        String[] paramNames = new String[1];
        paramNames[0] = "Minimum M/Z range width";

        NumberFormat[] numberFormats = new NumberFormat[1];
        numberFormats[0] = NumberFormat.getNumberInstance();
        numberFormats[0].setMinimumFractionDigits(3);

        logger.finest("Showing zoom scan filter parameter setup dialog");

        // Show parameter setup dialog
        ParameterSetupDialog psd = new ParameterSetupDialog(
                desktop.getMainWindow(), "Please check the parameter values",
                paramNames, paramValues, numberFormats);
        psd.setVisible(true);

        // Check if user clicked Cancel-button
        if (psd.getExitCode() == -1) {
            return null;
        }

        ZoomScanFilterParameters newParameters = new ZoomScanFilterParameters();

        // Read parameter values from dialog
        double d;

        // minMZRange
        d = psd.getFieldValue(0);
        if (d <= 0) {
            desktop.displayErrorMessage("Incorrect minimum M/Z range width!");
            return null;
        }
        newParameters.minMZRange = d;

        // save the current parameter settings for future runs
        currentProject.setParameters(this, newParameters);

        return newParameters;
    }

    /**
     * @see net.sf.mzmine.methods.Method#runMethod(net.sf.mzmine.methods.MethodParameters,
     *      net.sf.mzmine.io.RawDataFile[],
     *      net.sf.mzmine.methods.alignment.AlignmentResult[])
     */
    public void runMethod(MethodParameters parameters,
            MZmineOpenedFile[] dataFiles, AlignmentResult[] alignmentResults) {

        logger.finest("Running zoom scan filter");

        for (MZmineOpenedFile dataFile : dataFiles) {
            Task filterTask = new ZoomScanFilterTask(dataFile,
                    (ZoomScanFilterParameters) parameters);
            taskController.addTask(filterTask, this);
        }

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        MethodParameters parameters = askParameters();
        if (parameters == null)
            return;

        MZmineOpenedFile[] dataFiles = desktop.getSelectedDataFiles();

        runMethod(parameters, dataFiles, null);

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
            MZmineOpenedFile openedFile = (MZmineOpenedFile) result[0];
            RawDataFile newFile = (RawDataFile) result[1];
            MethodParameters cfParam = (MethodParameters) result[2];

            openedFile.updateFile(newFile, this, cfParam);

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            String msg = "Error while filtering a file: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);
        }

    }

    /**
     * @see net.sf.mzmine.methods.Method#getMethodDescription()
     */
    public String getMethodDescription() {
        return "Zoom scan filter";
    }

}