/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.rawdata.cropper;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Hashtable;
import java.util.logging.Logger;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

public class CropFilter implements BatchStep, TaskListener, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private CropFilterParameters parameters;

    private Desktop desktop;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        parameters = new CropFilterParameters();

        desktop.addMenuItem(
                MZmineMenu.RAWDATAPROCESSING,
                "Crop filter",
                "Cropping of the data set to given m/z and retention time ranges",
                KeyEvent.VK_C, this, null);

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        RawDataFile[] dataFiles = desktop.getSelectedDataFiles();
        if (dataFiles.length == 0) {
            desktop.displayErrorMessage("Please select at least one data file");
            return;
        }

        ExitCode exitCode = setupParameters(parameters);
        if (exitCode != ExitCode.OK)
            return;

        runModule(dataFiles, null, parameters.clone(), null);

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
    public ExitCode setupParameters(ParameterSet currentParameters) {

        Hashtable<Parameter, Object> autoValues = null;

        // If we have 1 selected data file, set the automatic parameter values
        RawDataFile[] dataFiles = desktop.getSelectedDataFiles();
        if (dataFiles.length == 1) {
            autoValues = new Hashtable<Parameter, Object>();
            autoValues.put(CropFilterParameters.retentionTimeRange,
                    dataFiles[0].getDataRTRange(1));
            autoValues.put(CropFilterParameters.mzRange,
                    dataFiles[0].getDataMZRange(1));

        }

        // Show dialog
        ParameterSetupDialog dialog = new ParameterSetupDialog(
                "Please set parameter values for " + toString(),
                (SimpleParameterSet) currentParameters, autoValues);
        dialog.setVisible(true);

        return dialog.getExitCode();
    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.data.RawDataFile[],
     *      net.sf.mzmine.data.PeakList[], net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.TaskGroupListener)
     */
    public TaskGroup runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
            ParameterSet parameters, TaskGroupListener taskGroupListener) {

        // check data files
        if ((dataFiles == null) || (dataFiles.length == 0)) {
            desktop.displayErrorMessage("Please select data files for filtering");
            return null;
        }

        // prepare a new task group
        Task tasks[] = new CropFilterTask[dataFiles.length];
        for (int i = 0; i < dataFiles.length; i++) {
            tasks[i] = new CropFilterTask(dataFiles[i],
                    (CropFilterParameters) parameters);
        }
        TaskGroup newGroup = new TaskGroup(tasks, this, taskGroupListener);

        // start this group
        newGroup.start();

        return newGroup;

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
        this.parameters = (CropFilterParameters) parameters;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskStarted(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskStarted(Task task) {
        CropFilterTask cropTask = (CropFilterTask) task;
        logger.info("Running cropping filter on " + cropTask.getDataFile());
    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {

        CropFilterTask cropTask = (CropFilterTask) task;

        if (task.getStatus() == Task.TaskStatus.FINISHED) {
            logger.info("Finished cropping filter on " + cropTask.getDataFile());
        }

        if (task.getStatus() == Task.TaskStatus.ERROR) {
            String msg = "Error while running cropping filter on "
                    + cropTask.getDataFile() + ": " + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);
        }

    }

    public BatchStepCategory getBatchStepCategory() {
        return BatchStepCategory.RAWDATAPROCESSING;
    }
}