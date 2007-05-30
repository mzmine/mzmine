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

package net.sf.mzmine.modules.filtering.crop;

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

public class CropFilter implements BatchStep, TaskListener,
        ActionListener {

    public static final Parameter parameterMSlevel = new SimpleParameter(
            ParameterType.INTEGER, "MS level",
            "MS level of scans to be filtered", null, new Object[] { 1, 2, 3,
                    4, 5, 6, 7, 8, 9, 10 });

    public static final Parameter parameterMinMZ = new SimpleParameter(
            ParameterType.DOUBLE, "Minimum M/Z",
            "Lower M/Z boundary of the cropped region", "Da",
            new Double(100.0), new Double(0.0), null);

    public static final Parameter parameterMaxMZ = new SimpleParameter(
            ParameterType.DOUBLE, "Maximum M/Z",
            "Upper M/Z boundary of the cropped region", "Da",
            new Double(1000.0), new Double(0.0), null);

    public static final Parameter parameterMinRT = new SimpleParameter(
            ParameterType.DOUBLE, "Minimum Retention time",
            "Lower RT boundary of the cropped region", "seconds", new Double(
                    0.0), new Double(0.0), null);

    public static final Parameter parameterMaxRT = new SimpleParameter(
            ParameterType.DOUBLE, "Maximum Retention time",
            "Upper RT boundary of the cropped region", "seconds", new Double(
                    600.0), new Double(0.0), null);

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
                new Parameter[] { parameterMSlevel, parameterMinMZ,
                        parameterMaxMZ, parameterMinRT, parameterMaxRT });

        desktop.addMenuItem(MZmineMenu.FILTERING, "Crop filter", this, null,
                KeyEvent.VK_C, false, true);

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
        return "Cropping filter";
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
        Task tasks[] = new CropFilterTask[dataFiles.length];
        for (int i = 0; i < dataFiles.length; i++) {
            tasks[i] = new CropFilterTask(dataFiles[i], parameters);
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
        logger.info("Running cropping filter on "
                + ((CropFilterTask) task).getDataFile());
    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

            logger.info("Finished cropping filter on "
                    + ((CropFilterTask) task).getDataFile());

            OpenedRawDataFile openedFile = ((CropFilterTask) task).getDataFile();
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