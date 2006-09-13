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

package net.sf.mzmine.methods.peakpicking.recursivethreshold;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
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

public class RecursiveThresholdPicker implements Method,
        TaskListener, ListSelectionListener, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private TaskController taskController;
    private Desktop desktop;
    private JMenuItem myMenuItem;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {

        this.taskController = core.getTaskController();
        this.desktop = core.getDesktop();

        myMenuItem = desktop.addMenuItem(MZmineMenu.PEAKPICKING,
                "Recursive threshold peak detector", this, null, KeyEvent.VK_R,
                false, false);

        desktop.addSelectionListener(this);

    }

    /**
     * This function displays a modal dialog to define method parameters
     *
     * @see net.sf.mzmine.methods.Method#askParameters()
     */
    public MethodParameters askParameters() {

        MZmineProject currentProject = MZmineProject.getCurrentProject();
        RecursiveThresholdPickerParameters currentParameters = (RecursiveThresholdPickerParameters) currentProject.getParameters(this);
        if (currentParameters == null)
            currentParameters = new RecursiveThresholdPickerParameters();

        // Initialize parameter setup dialog
        double[] paramValues = new double[9];
        paramValues[0] = currentParameters.binSize;
        paramValues[1] = currentParameters.chromatographicThresholdLevel;
        paramValues[2] = currentParameters.noiseLevel;
        paramValues[3] = currentParameters.minimumPeakHeight;
        paramValues[4] = currentParameters.minimumPeakDuration;
        paramValues[5] = currentParameters.minimumMZPeakWidth;
        paramValues[6] = currentParameters.maximumMZPeakWidth;
        paramValues[7] = currentParameters.mzTolerance;
        paramValues[8] = currentParameters.intTolerance;

        String[] paramNames = new String[9];
        paramNames[0] = "M/Z bin size (Da)";
        paramNames[1] = "Chromatographic threshold level (%)";
        paramNames[2] = "Noise level (absolute value)";
        paramNames[3] = "Minimum peak height (absolute value)";
        paramNames[4] = "Minimum peak duration (seconds)";
        paramNames[5] = "Minimum M/Z peak width (Da)";
        paramNames[6] = "Maximum M/Z peak width (Da)";
        paramNames[7] = "Tolerance for M/Z variation (Da)";
        paramNames[8] = "Tolerance for intensity variation (%)";

        NumberFormat[] numberFormats = new NumberFormat[9];
        numberFormats[0] = NumberFormat.getNumberInstance();
        numberFormats[0].setMinimumFractionDigits(3);
        numberFormats[1] = NumberFormat.getPercentInstance();
        numberFormats[2] = NumberFormat.getNumberInstance();
        numberFormats[2].setMinimumFractionDigits(0);
        numberFormats[3] = NumberFormat.getNumberInstance();
        numberFormats[3].setMinimumFractionDigits(0);
        numberFormats[4] = NumberFormat.getNumberInstance();
        numberFormats[4].setMinimumFractionDigits(1);
        numberFormats[5] = NumberFormat.getNumberInstance();
        numberFormats[5].setMinimumFractionDigits(3);
        numberFormats[6] = NumberFormat.getNumberInstance();
        numberFormats[6].setMinimumFractionDigits(3);
        numberFormats[7] = NumberFormat.getNumberInstance();
        numberFormats[7].setMinimumFractionDigits(3);
        numberFormats[8] = NumberFormat.getPercentInstance();

        logger.finest("Showing recursive threshold peak picker parameter setup dialog");

        // Show parameter setup dialog
        ParameterSetupDialog psd = new ParameterSetupDialog(
                desktop.getMainFrame(), "Please check the parameter values",
                paramNames, paramValues, numberFormats);
        psd.setVisible(true);

        // Check if user clicked Cancel-button
        if (psd.getExitCode() == -1) {
            return null;
        }

        RecursiveThresholdPickerParameters newParameters = new RecursiveThresholdPickerParameters();

        // Read parameter values from dialog
        double d;

        d = psd.getFieldValue(0);
        if (d <= 0) {
            desktop.displayErrorMessage("Incorrect " + paramNames[0]);
            return null;
        }
        currentParameters.binSize = d;

        d = psd.getFieldValue(1);
        if (d < 0) {
            desktop.displayErrorMessage("Incorrect " + paramNames[1]);
            return null;
        }
        currentParameters.chromatographicThresholdLevel = d;

        d = psd.getFieldValue(2);
        if (d < 0) {
            desktop.displayErrorMessage("Incorrect " + paramNames[2]);
            return null;
        }
        currentParameters.noiseLevel = d;

        d = psd.getFieldValue(3);
        if (d <= 0) {
            desktop.displayErrorMessage("Incorrect " + paramNames[3]);
            return null;
        }
        currentParameters.minimumPeakHeight = d;

        d = psd.getFieldValue(4);
        if (d <= 0) {
            desktop.displayErrorMessage("Incorrect " + paramNames[4]);
            return null;
        }
        currentParameters.minimumPeakDuration = d;

        d = psd.getFieldValue(5);
        if (d <= 0) {
            desktop.displayErrorMessage("Incorrect " + paramNames[5]);
            return null;
        }
        currentParameters.minimumMZPeakWidth = d;

        d = psd.getFieldValue(6);
        if (d <= 0) {
            desktop.displayErrorMessage("Incorrect " + paramNames[6]);
            return null;
        }
        currentParameters.maximumMZPeakWidth = d;

        d = psd.getFieldValue(7);
        if (d < 0) {
            desktop.displayErrorMessage("Incorrect " + paramNames[7]);
            return null;
        }
        currentParameters.mzTolerance = d;

        d = psd.getFieldValue(8);
        if (d < 0) {
            desktop.displayErrorMessage("Incorrect " + paramNames[8]);
            return null;
        }
        currentParameters.intTolerance = d;

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
            OpenedRawDataFile[] dataFiles, AlignmentResult[] alignmentResults) {

        logger.info("Running recursive threshold peak picker");

        for (OpenedRawDataFile dataFile : dataFiles) {
            Task pickerTask = new RecursiveThresholdPickerTask(dataFile,
                    (RecursiveThresholdPickerParameters) parameters);
            taskController.addTask(pickerTask, this);
        }

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        MethodParameters parameters = askParameters();
        if (parameters == null)
            return;

        OpenedRawDataFile[] dataFiles = desktop.getSelectedDataFiles();

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
            OpenedRawDataFile dataFile = (OpenedRawDataFile) result[0];
            PeakList peakList = (PeakList) result[1];
            MethodParameters params = (MethodParameters) result[2];

            // Add peak picking to the history of the file
            dataFile.addHistoryEntry(dataFile.getCurrentFile().getFile(), this,
                    params);

            // Add peak list as data unit to current file
            dataFile.getCurrentFile().addData(PeakList.class, peakList);

			// Notify listeners
			MainWindow.getInstance().getItemSelector().fireDataChanged();

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            String msg = "Error while peak picking a file: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);
        }

    }

    /**
     * @see net.sf.mzmine.methods.Method#toString()
     */
    public String toString() {
        return "Recursive threshold peak detector";
    }

}
