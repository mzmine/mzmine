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

package net.sf.mzmine.methods.alignment.join;

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
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.TaskSequence;
import net.sf.mzmine.taskcontrol.TaskSequenceListener;
import net.sf.mzmine.taskcontrol.TaskSequence.TaskSequenceStatus;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog.ExitCode;

/**
 * 
 */
public class JoinAligner implements Method, TaskListener,
        ListSelectionListener, ActionListener {

    public static final String RTToleranceTypeAbsolute = "Absolute";
    public static final String RTToleranceTypeRelative = "Relative";

    public static final Object[] RTToleranceTypePossibleValues = {
            RTToleranceTypeAbsolute, RTToleranceTypeRelative };

    public static final Parameter MZvsRTBalance = new SimpleParameter(
            ParameterType.DOUBLE, "M/Z vs RT balance",
            "Used in distance measuring as multiplier of M/Z difference", "",
            new Double(10.0), new Double(0.0), null);

    public static final Parameter MZTolerance = new SimpleParameter(
            ParameterType.DOUBLE, "M/Z tolerance",
            "Maximum allowed M/Z difference", "Da", new Double(0.2),
            new Double(0.0), null);

    public static final Parameter RTToleranceType = new SimpleParameter(
            ParameterType.STRING,
            "RT tolerance type",
            "Maximum RT difference can be defined either using absolute or relative value",
            RTToleranceTypeAbsolute, RTToleranceTypePossibleValues);

    public static final Parameter RTToleranceValueAbs = new SimpleParameter(
            ParameterType.DOUBLE, "Absolute RT tolerance",
            "Maximum allowed absolute RT difference", "seconds", new Double(
                    15.0), new Double(0.0), null);

    public static final Parameter RTToleranceValuePercent = new SimpleParameter(
            ParameterType.DOUBLE, "Relative RT tolerance",
            "Maximum allowed relative RT difference", "%", new Double(0.15),
            new Double(0.0), null);

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

        parameters = new SimpleParameterSet(new Parameter[] { MZvsRTBalance,
                MZTolerance, RTToleranceType, RTToleranceValueAbs,
                RTToleranceValuePercent });

        myMenuItem = desktop.addMenuItem(MZmineMenu.ALIGNMENT,
                "Peak list aligner", this, null, KeyEvent.VK_A, false, false);

        desktop.addSelectionListener(this);

    }

    public String toString() {
        return new String("Join Aligner");
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        return parameters;
    }

    public void setParameters(ParameterSet parameters) {
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

        boolean allOk = true;

        for (OpenedRawDataFile file : dataFiles) {
            if (!file.getCurrentFile().hasData(PeakList.class)) {
                allOk = false;
                break;
            }
        }
        myMenuItem.setEnabled(allOk);

    }

    public void taskStarted(Task task) {
        logger.info("Running join aligner");
    }

    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

            logger.info("Finished join aligner");

            AlignmentResult alignmentResult = (AlignmentResult) task.getResult();

            MZmineProject.getCurrentProject().addAlignmentResult(
                    alignmentResult);

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            String msg = "Error while aligning peak lists: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);

        }

    }

    /**
     * @see net.sf.mzmine.methods.Method#runMethod(net.sf.mzmine.io.OpenedRawDataFile[],
     *      net.sf.mzmine.data.AlignmentResult[],
     *      net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.TaskSequenceListener)
     */
    public TaskSequence runMethod(OpenedRawDataFile[] dataFiles,
            AlignmentResult[] alignmentResults, ParameterSet parameters,
            TaskSequenceListener methodListener) {

        // check peaklists
        for (int i = 0; i < dataFiles.length; i++) {
            if (dataFiles[i].getCurrentFile().getData(PeakList.class).length == 0) {
                String msg = "Cannot start alignment of " + dataFiles[i]
                        + ", please run peak picking first.";
                logger.severe(msg);
                desktop.displayErrorMessage(msg);
                return null;
            }
        }

        // prepare a new sequence with just one task
        Task tasks[] = new JoinAlignerTask[1];
        tasks[0] = new JoinAlignerTask(dataFiles, parameters);
        TaskSequence newSequence = new TaskSequence(tasks, this,
                methodListener, taskController);

        // execute the sequence
        newSequence.run();
        
        return newSequence;

    }

}
