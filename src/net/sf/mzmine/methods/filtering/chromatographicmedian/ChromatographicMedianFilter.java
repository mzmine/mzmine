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

package net.sf.mzmine.methods.filtering.chromatographicmedian;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.AlignmentResult;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
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

public class ChromatographicMedianFilter implements Method,
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

        myMenuItem = desktop.addMenuItem(MZmineMenu.FILTERING,
                "Chromatographic median filter", this, null, KeyEvent.VK_H,
                false, false);

        desktop.addSelectionListener(this);

    }

    /**
     * @see net.sf.mzmine.methods.Method#askParameters()
     */
    public MethodParameters askParameters() {

        MZmineProject currentProject = MZmineProject.getCurrentProject();
        ChromatographicMedianFilterParameters currentParameters = (ChromatographicMedianFilterParameters) currentProject.getParameters(this);
        if (currentParameters == null)
            currentParameters = new ChromatographicMedianFilterParameters();

        // Initialize parameter setup dialog
        double[] paramValues = new double[2];
        paramValues[0] = currentParameters.mzTolerance;
        paramValues[1] = currentParameters.oneSidedWindowLength;

        String[] paramNames = new String[2];
        paramNames[0] = "Tolerance in M/Z tolerance (Da)";
        paramNames[1] = "One-sided window length (scans)";

        NumberFormat[] numberFormats = new NumberFormat[2];
        numberFormats[0] = NumberFormat.getNumberInstance();
        numberFormats[0].setMinimumFractionDigits(3);
        numberFormats[1] = NumberFormat.getIntegerInstance();

        logger.finest("Showing cromatographic median filter parameter setup dialog");

        // Show parameter setup dialog
        ParameterSetupDialog psd = new ParameterSetupDialog(
                desktop.getMainFrame(), "Please check the parameter values",
                paramNames, paramValues, numberFormats);
        psd.setVisible(true);

        // Check if user clicked Cancel-button
        if (psd.getExitCode() == -1) {
            return null;
        }

        ChromatographicMedianFilterParameters newParameters = new ChromatographicMedianFilterParameters();

        // Write values from dialog back to parameters object
        double d;

        d = psd.getFieldValue(0);
        if (d <= 0) {
            desktop.displayErrorMessage("Incorrect M/Z tolerance value!");
            return null;
        }
        newParameters.mzTolerance = d;

        int i;
        i = (int) Math.round(psd.getFieldValue(1));
        if (i <= 0) {
            desktop.displayErrorMessage("Incorrect one-sided scan window length!");
            return null;
        }
        newParameters.oneSidedWindowLength = i;

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

        logger.finest("Running chromatographic median filter");

        for (OpenedRawDataFile dataFile : dataFiles) {
            Task filterTask = new ChromatographicMedianFilterTask(dataFile,
                    (ChromatographicMedianFilterParameters) parameters);
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
            OpenedRawDataFile openedFile = (OpenedRawDataFile) result[0];
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
     * @see net.sf.mzmine.methods.Method#toString()
     */
    public String toString() {
        return "Chromatographic median filter";
    }

}
