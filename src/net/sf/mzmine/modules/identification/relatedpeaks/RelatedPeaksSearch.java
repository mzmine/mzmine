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

package net.sf.mzmine.modules.identification.relatedpeaks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

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

/**
 * @author Alex
 * 
 */
public class RelatedPeaksSearch implements BatchStep, ActionListener,
        TaskListener {

    public static final String MODULE_NAME = "Related peaks search";

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private Desktop desktop;

    private RelatedPeaksSearchParameters parameters;

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        return parameters;
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {
        this.desktop = MZmineCore.getDesktop();

        parameters = new RelatedPeaksSearchParameters();

        desktop.addMenuItem(
                MZmineMenu.IDENTIFICATION,
                MODULE_NAME,
                "Identification of related peaks by mass and retention time throw the same raw data",
                KeyEvent.VK_R, false, this, null);

    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public void setParameters(ParameterSet parameterValues) {
        this.parameters = (RelatedPeaksSearchParameters) parameterValues;
    }

    /**
     * @see net.sf.mzmine.modules.batchmode.BatchStep#getBatchStepCategory()
     */
    public BatchStepCategory getBatchStepCategory() {
        return BatchStepCategory.IDENTIFICATION;
    }

    /**
     * @see net.sf.mzmine.modules.batchmode.BatchStep#setupParameters(net.sf.mzmine.data.ParameterSet)
     */
    public ExitCode setupParameters(ParameterSet parameters) {
        ParameterSetupDialog dialog = new ParameterSetupDialog(
                "Please set parameter values for " + toString(),
                (SimpleParameterSet) parameters);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        PeakList[] peaklists = desktop.getSelectedPeakLists();

        if (peaklists.length == 0) {
            desktop.displayErrorMessage("Please select a peak lists to process");
            return;
        }

        RelatedPeaksSearchDialog dialog = new  RelatedPeaksSearchDialog(parameters);
        dialog.setVisible(true);

        if (dialog.getExitCode() != ExitCode.OK) {
            return;
        }
        
        runModule(null, peaklists, parameters.clone(), null);

    }

    /**
     * @see net.sf.mzmine.modules.batchmode.BatchStep#runModule(net.sf.mzmine.data.RawDataFile[],
     *      net.sf.mzmine.data.PeakList[], net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.TaskGroupListener)
     */
    public TaskGroup runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
            ParameterSet parameters, TaskGroupListener taskGroupListener) {
        if (peakLists == null) {
            throw new IllegalArgumentException(
                    "Cannot run identification without a peak list");
        }

        // prepare a new sequence of tasks
        Task tasks[] = new RelatedPeaksSearchTask[peakLists.length];
        for (int i = 0; i < peakLists.length; i++) {
            tasks[i] = new RelatedPeaksSearchTask(
                    (RelatedPeaksSearchParameters) parameters, peakLists[i]);
        }
        TaskGroup newSequence = new TaskGroup(tasks, this, taskGroupListener);

        // execute the sequence
        newSequence.start();

        return newSequence;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "related peaks search";
    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskStarted(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskStarted(Task task) {
        logger.info("Running related peaks search");
    }

    /**
     * @see net.sf.mzmine.taskcontrol.TaskListener#taskFinished(net.sf.mzmine.taskcontrol.Task)
     */
    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {
            logger.info("Finished realted peaks searching");
        }

        if (task.getStatus() == Task.TaskStatus.ERROR) {

            String msg = "Error while searching ofr related peaks: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);

        }

    }

}
