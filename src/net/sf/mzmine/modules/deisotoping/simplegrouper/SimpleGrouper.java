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

package net.sf.mzmine.modules.deisotoping.simplegrouper;

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
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.DataProcessingMethod;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;

/**
 * This class implements a simple isotopic peaks grouper method based on
 * searhing for neighbouring peaks from expected locations.
 * 
 */

public class SimpleGrouper implements DataProcessingMethod, TaskListener,
        ListSelectionListener, ActionListener {

    public static final Parameter mzTolerance = new SimpleParameter(
            ParameterType.DOUBLE, "M/Z tolerance",
            "Maximum distance in M/Z from the expected location of a peak",
            "Da", new Double(0.05), new Double(0.0), null);

    public static final Parameter rtTolerance = new SimpleParameter(
            ParameterType.DOUBLE, "RT tolerance",
            "Maximum distance in RT from the expected location of a peak",
            "seconds", new Double(5.0), new Double(0.0), null);

    public static final Parameter monotonicShape = new SimpleParameter(
            ParameterType.BOOLEAN,
            "Monotonic shape",
            "If true, then monotonically decreasing height of isotope pattern is required (monoisotopic peak is strongest).",
            new Boolean(true));

    public static final Parameter maximumCharge = new SimpleParameter(
            ParameterType.INTEGER, "Maximum charge", "Maximum charge", "",
            new Integer(1), new Integer(1), null);

    private ParameterSet parameters;

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

        parameters = new SimpleParameterSet(new Parameter[] { mzTolerance,
                rtTolerance, monotonicShape, maximumCharge });

        myMenuItem = desktop.addMenuItem(MZmineMenu.PEAKPICKING,
                "Simple isotopic peaks grouper", this, null, KeyEvent.VK_S,
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

        OpenedRawDataFile[] dataFiles = desktop.getSelectedDataFiles();

        for (OpenedRawDataFile file : dataFiles) {
            if (file.getPeakList() != null) {
                myMenuItem.setEnabled(true);
                return;
            }
        }
        myMenuItem.setEnabled(false);
    }

    public void taskStarted(Task task) {
        logger.info("Running simple peak grouper on "
                + ((SimpleGrouperTask) task).getDataFile());
    }

    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

            logger.info("Finished simple peak grouper on "
                    + ((SimpleGrouperTask) task).getDataFile());

            Object[] result = (Object[]) task.getResult();
            OpenedRawDataFile dataFile = (OpenedRawDataFile) result[0];
            PeakList peakList = (PeakList) result[1];
            SimpleParameterSet params = (SimpleParameterSet) result[2];

            dataFile.addHistoryEntry(dataFile.getCurrentFile().getFile(), this,
                    params);

            // Add peak list to MZmineProject
            dataFile.setPeakList(peakList);

            // Notify listeners
            desktop.notifySelectionListeners();

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            String msg = "Error while deisotoping a file: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);

        }

    }

    /**
     * @see net.sf.mzmine.modules.DataProcessingMethod#toString()
     */
    public String toString() {
        return "Simple isotopic peaks grouper";
    }

    /**
     * @see net.sf.mzmine.modules.DataProcessingMethod#setupParameters(net.sf.mzmine.data.ParameterSet)
     */
    public ParameterSet setupParameters(ParameterSet currentParameters) {
        ParameterSetupDialog dialog = new ParameterSetupDialog(
                desktop.getMainFrame(), "Please check parameter values for "
                        + toString(), (SimpleParameterSet) currentParameters);
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
     * @see net.sf.mzmine.modules.DataProcessingMethod#runMethod(net.sf.mzmine.io.OpenedRawDataFile[],
     *      net.sf.mzmine.data.AlignmentResult[],
     *      net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.TaskGroupListener)
     */
    public TaskGroup runMethod(OpenedRawDataFile[] dataFiles,
            AlignmentResult[] alignmentResults, ParameterSet parameters,
            TaskGroupListener methodListener) {

        // prepare a new sequence of tasks
        Task tasks[] = new SimpleGrouperTask[dataFiles.length];
        for (int i = 0; i < dataFiles.length; i++) {

            if (dataFiles[i].getPeakList() == null) {
                String msg = "Cannot start deisotoping of " + dataFiles[i]
                        + ", please run peak picking first.";
                logger.severe(msg);
                desktop.displayErrorMessage(msg);
                return null;
            }
            tasks[i] = new SimpleGrouperTask(dataFiles[i], parameters);
        }

        TaskGroup newSequence = new TaskGroup(tasks, this, methodListener,
                taskController);

        // execute the sequence
        newSequence.run();

        return newSequence;

    }

}
