/*
 * Copyright 2006-2007 The MZmine Development Team
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
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog.ExitCode;

public class RecursivePicker implements Method, TaskListener,
        ListSelectionListener, ActionListener {

    public static final NumberFormat percentFormat = NumberFormat.getPercentInstance();
    
    public static final Parameter binSize = new SimpleParameter(
            ParameterType.DOUBLE, "M/Z bin width",
            "Width of M/Z range for each precalculated XIC", "Da", new Double(
                    0.25), new Double(0.05), null);

    public static final Parameter chromatographicThresholdLevel = new SimpleParameter(
            ParameterType.DOUBLE, "Chromatographic threshold level",
            "Used in defining threshold level value from an XIC", "%",
            new Double(0.0), new Double(0.0), new Double(1.0), percentFormat);

    public static final Parameter noiseLevel = new SimpleParameter(
            ParameterType.DOUBLE, "Noise level",
            "Intensities less than this value are interpreted as noise",
            "absolute", new Double(10.0), new Double(0.0), null);

    public static final Parameter minimumPeakHeight = new SimpleParameter(
            ParameterType.DOUBLE, "Min peak height",
            "Minimum acceptable peak height", "absolute", new Double(100.0),
            new Double(0.0), null);

    public static final Parameter minimumPeakDuration = new SimpleParameter(
            ParameterType.DOUBLE, "Min peak duration",
            "Minimum acceptable peak duration", "seconds", new Double(4.0),
            new Double(0.0), null);

    public static final Parameter minimumMZPeakWidth = new SimpleParameter(
            ParameterType.DOUBLE, "Min M/Z peak width",
            "Minimum acceptable peak width in M/Z", "Da", new Double(0.2),
            new Double(0.0), null);

    public static final Parameter maximumMZPeakWidth = new SimpleParameter(
            ParameterType.DOUBLE, "Max M/Z peak width",
            "Maximum acceptable peak width in M/Z", "Da", new Double(1.00),
            new Double(0.0), null);

    public static final Parameter mzTolerance = new SimpleParameter(
            ParameterType.DOUBLE,
            "M/Z tolerance",
            "Maximum allowed distance in M/Z between centroid peaks in successive scans",
            "Da", new Double(0.1), new Double(0.0), null);

    public static final Parameter intTolerance = new SimpleParameter(
            ParameterType.DOUBLE,
            "Intensity tolerance",
            "Maximum allowed deviation from expected /\\ shape of a peak in chromatographic direction",
            "%", new Double(0.15), new Double(0.0), null, percentFormat);

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private ParameterSet parameters;

    private TaskController taskController;
    private Desktop desktop;
    private JMenuItem myMenuItem;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {

        this.taskController = core.getTaskController();
        this.desktop = core.getDesktop();

        parameters = new SimpleParameterSet(new Parameter[] { binSize,
                chromatographicThresholdLevel, noiseLevel, minimumPeakHeight,
                minimumPeakDuration, minimumMZPeakWidth, maximumMZPeakWidth,
                mzTolerance, intTolerance });

        myMenuItem = desktop.addMenuItem(MZmineMenu.PEAKPICKING,
                "Recursive threshold peak detector", this, null, KeyEvent.VK_R,
                false, false);

        desktop.addSelectionListener(this);

    }

    public void setParameters(ParameterSet parameters) {
        this.parameters = parameters;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == myMenuItem) {
            ParameterSet param = setupParameters(parameters);
            if (param == null)
                return;
            OpenedRawDataFile[] dataFiles = desktop.getSelectedDataFiles();
            runMethod(dataFiles, null, param, null);
        }
    }

    /**
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {
        myMenuItem.setEnabled(desktop.isDataFileSelected());
    }

    public void taskStarted(Task task) {
        logger.info("Running recursive threshold peak picker on "
                + ((RecursivePickerTask) task).getDataFile());

    }

    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

            logger.info("Finished recursive threshold peak picker on "
                    + ((RecursivePickerTask) task).getDataFile());

            Object[] result = (Object[]) task.getResult();
            OpenedRawDataFile dataFile = (OpenedRawDataFile) result[0];
            PeakList peakList = (PeakList) result[1];
            SimpleParameterSet params = (SimpleParameterSet) result[2];

            // Add peak picking to the history of the file
            dataFile.addHistoryEntry(dataFile.getCurrentFile().getFile(), this,
                    params);

            // Add peak list as data unit to current file
            dataFile.getCurrentFile().addData(PeakList.class, peakList);

            // Notify listeners
            desktop.notifySelectionListeners();

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

    /**
     * @see net.sf.mzmine.main.MZmineModule#setCurrentParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setCurrentParameters(ParameterSet parameters) {
        this.parameters = parameters;
    }

    /**
     * @see net.sf.mzmine.methods.Method#setupParameters(net.sf.mzmine.data.ParameterSet)
     */
    public ParameterSet setupParameters(ParameterSet currentParameters) {
        ParameterSetupDialog dialog = new ParameterSetupDialog(
                desktop.getMainFrame(), "Please check parameter values for "
                        + toString(), currentParameters);
        dialog.setVisible(true);
        if (dialog.getExitCode() == ExitCode.CANCEL)
            return null;
        return currentParameters.clone();
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        return parameters;
    }

    /**
     * @see net.sf.mzmine.methods.Method#runMethod(net.sf.mzmine.io.OpenedRawDataFile[],
     *      net.sf.mzmine.data.AlignmentResult[],
     *      net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.TaskGroupListener)
     */
    public TaskGroup runMethod(OpenedRawDataFile[] dataFiles,
            AlignmentResult[] alignmentResults, ParameterSet parameters,
            TaskGroupListener methodListener) {

        // prepare a new sequence of tasks
        Task tasks[] = new RecursivePickerTask[dataFiles.length];
        for (int i = 0; i < dataFiles.length; i++) {
            tasks[i] = new RecursivePickerTask(dataFiles[i],
                    parameters);
        }
        TaskGroup newSequence = new TaskGroup(tasks, this,
                methodListener, taskController);

        // execute the sequence
        newSequence.run();
        
        return newSequence;

    }

}
