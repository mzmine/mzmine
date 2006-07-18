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

package net.sf.mzmine.methods.peakpicking.local;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.IOController;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
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
import net.sf.mzmine.visualizers.peaklist.table.PeakListTableView;

/**
 * This class implements a peak picker based on searching for local maximums in
 * each spectra
 */
public class LocalPicker implements MZmineModule, Method, TaskListener,
        ListSelectionListener, ActionListener {

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
                "Local maxima peak detector", this, null, KeyEvent.VK_L, false,
                false);

        desktop.addSelectionListener(this);

    }

    /**
     * This function displays a modal dialog to define method parameters
     * 
     * @see net.sf.mzmine.methods.Method#askParameters()
     */
    public MethodParameters askParameters() {

        MZmineProject currentProject = MZmineProject.getCurrentProject();
        LocalPickerParameters currentParameters = (LocalPickerParameters) currentProject.getParameters(this);
        if (currentParameters == null)
            currentParameters = new LocalPickerParameters();

        // Ínitialize parameter setup dialog
        double[] paramValues = new double[7];
        paramValues[0] = currentParameters.binSize;
        paramValues[1] = currentParameters.chromatographicThresholdLevel;
        paramValues[2] = currentParameters.noiseLevel;
        paramValues[3] = currentParameters.minimumPeakHeight;
        paramValues[4] = currentParameters.minimumPeakDuration;
        paramValues[5] = currentParameters.mzTolerance;
        paramValues[6] = currentParameters.intTolerance;

        String[] paramNames = new String[7];
        paramNames[0] = "M/Z bin size (Da)";
        paramNames[1] = "Chromatographic threshold level (%)";
        paramNames[2] = "Noise level (absolute value)";
        paramNames[3] = "Minimum peak height (absolute value)";
        paramNames[4] = "Minimum peak duration (seconds)";
        paramNames[5] = "Tolerance for m/z variation (Da)";
        paramNames[6] = "Tolerance for intensity variation (%)";

        NumberFormat[] numberFormats = new NumberFormat[7];
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
        numberFormats[6] = NumberFormat.getPercentInstance();

        logger.finest("Showing local maxima peak picker parameter setup dialog");

        // Show parameter setup dialog
        ParameterSetupDialog psd = new ParameterSetupDialog(
                desktop.getMainWindow(), "Please check the parameter values",
                paramNames, paramValues, numberFormats);
        psd.setVisible(true);

        // Check if user clicked Cancel-button
        if (psd.getExitCode() == -1) {
            return null;
        }

        LocalPickerParameters newParameters = new LocalPickerParameters();

        // Read parameter values
        double d;

        d = psd.getFieldValue(0);
        if (d <= 0) {
            desktop.displayErrorMessage("Incorrect bin size!");
            return null;
        }
        newParameters.binSize = d;

        d = psd.getFieldValue(1);
        if ((d < 0) || (d > 1)) {
            desktop.displayErrorMessage("Incorrect chromatographic threshold level!");
            return null;
        }
        newParameters.chromatographicThresholdLevel = d;

        d = psd.getFieldValue(2);
        if (d < 0) {
            desktop.displayErrorMessage("Incorrect noise level!");
            return null;
        }
        newParameters.noiseLevel = d;

        d = psd.getFieldValue(3);
        if (d <= 0) {
            desktop.displayErrorMessage("Incorrect minimum peak height!");
            return null;
        }
        newParameters.minimumPeakHeight = d;

        d = psd.getFieldValue(4);
        if (d <= 0) {
            desktop.displayErrorMessage("Incorrect minimum peak duration!");
            return null;
        }
        newParameters.minimumPeakDuration = d;

        d = psd.getFieldValue(5);
        if (d < 0) {
            desktop.displayErrorMessage("Incorrect m/z tolerance value!");
            return null;
        }
        newParameters.mzTolerance = d;

        d = psd.getFieldValue(6);
        if (d < 0) {
            desktop.displayErrorMessage("Incorrect intensity tolerance value!");
            return null;
        }
        newParameters.intTolerance = d;

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

        logger.finest("Running local maxima peak picker");

        for (OpenedRawDataFile dataFile : dataFiles) {
            Task pickerTask = new LocalPickerTask(dataFile,
                    (LocalPickerParameters) parameters);
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

            // Add peak list to MZmineProject
            MZmineProject.getCurrentProject().setPeakList(dataFile, peakList);

            PeakListTableView peakListTable = new PeakListTableView(dataFile);
            desktop.addInternalFrame(peakListTable);

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            String msg = "Error while peak picking a file: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);
        }

    }

    /**
     * @see net.sf.mzmine.methods.Method#getMethodDescription()
     */
    public String getMethodDescription() {
        return "Local maxima peak detector";
    }

}
