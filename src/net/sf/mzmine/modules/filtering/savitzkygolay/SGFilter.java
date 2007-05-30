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

package net.sf.mzmine.modules.filtering.savitzkygolay;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.BatchStep;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskController;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;

public class SGFilter implements BatchStep, TaskListener,
        ActionListener {

    public static final Parameter parameterDatapoints = new SimpleParameter(
            ParameterType.INTEGER, "Number of datapoints",
            "Number of datapoints", 5, new Object[] { 5, 7, 9, 11, 13, 15 });

    private ParameterSet parameters;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private TaskController taskController;
    private Desktop desktop;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule(MZmineCore core) {

        this.taskController = core.getTaskController();
        this.desktop = core.getDesktop();

        parameters = new SimpleParameterSet(
                new Parameter[] { parameterDatapoints });

        desktop.addMenuItem(MZmineMenu.FILTERING,
                "Savitzky-Golay filter spectra", this, null, KeyEvent.VK_S,
                false, true);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        OpenedRawDataFile[] dataFiles = desktop.getSelectedDataFiles();
        if (dataFiles.length == 0) {
            desktop.displayErrorMessage("Please select at least one data file");
            return;
        }

        ParameterSet param = setupParameters(parameters);
        if (param == null)
            return;

        runModule(dataFiles, null, param, null);

    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#toString()
     */
    public String toString() {
        return "Savitzky-Golay filter";
    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#setupParameters()
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
     * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.io.OpenedRawDataFile[],
     *      net.sf.mzmine.data.PeakList[],
     *      net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.TaskGroupListener)
     */
    public TaskGroup runModule(OpenedRawDataFile[] dataFiles,
            PeakList[] alignmentResults, ParameterSet parameters,
            TaskGroupListener methodListener) {

        // prepare a new sequence of tasks
        Task tasks[] = new SGFilterTask[dataFiles.length];
        for (int i = 0; i < dataFiles.length; i++) {
            tasks[i] = new SGFilterTask(dataFiles[i], parameters);
        }
        TaskGroup newSequence = new TaskGroup(tasks, this, methodListener,
                taskController);

        // execute the sequence
        newSequence.run();

        return newSequence;

    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        return parameters;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameters) {
        this.parameters = parameters;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskStarted(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskStarted(Task task) {
        logger.info("Running Savitzky-Golay filter on "
                + ((SGFilterTask) task).getDataFile());
    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

            logger.info("Finished Savitzky-Golay filter on "
                    + ((SGFilterTask) task).getDataFile());

            OpenedRawDataFile openedFile = ((SGFilterTask) task).getDataFile();
            RawDataFile newFile = (RawDataFile) task.getResult();

            openedFile.updateFile(newFile, this, parameters);

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            String msg = "Error while filtering a file: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);

        }

    }

}
