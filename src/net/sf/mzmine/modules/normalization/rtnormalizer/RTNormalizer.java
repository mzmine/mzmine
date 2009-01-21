/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.normalization.rtnormalizer;

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
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * 
 */
public class RTNormalizer implements BatchStep, TaskListener, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private RTNormalizerParameters parameters;

    private Desktop desktop;

    /**
     * @see net.sf.mzmine.main.mzmineclient.MZmineModule#initModule(net.sf.mzmine.main.mzmineclient.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        parameters = new RTNormalizerParameters();

        desktop.addMenuItem(MZmineMenu.NORMALIZATION,
                "Retention time normalizer",
                "Retention time normalization using common, high intensity peaks",
                KeyEvent.VK_R, false, this, null);

    }

    public String toString() {
        return "Retention time normalizer";
    }

    /**
     * @see net.sf.mzmine.main.mzmineclient.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        return parameters;
    }

    public void setParameters(ParameterSet parameters) {
        this.parameters = (RTNormalizerParameters) parameters;
    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#setupParameters(net.sf.mzmine.data.ParameterSet)
     */
    public ExitCode setupParameters(ParameterSet currentParameters) {
        ParameterSetupDialog dialog = new ParameterSetupDialog(
                "Please set parameter values for " + toString(),
                (SimpleParameterSet) currentParameters);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        PeakList[] selectedPeakLists = desktop.getSelectedPeakLists();

        // check peak lists
        if ((selectedPeakLists == null) || (selectedPeakLists.length < 2)) {
            desktop.displayErrorMessage("Please select at least 2 peak lists for normalization");
            return;
        }

        ExitCode exitCode = setupParameters(parameters);
        if (exitCode != ExitCode.OK)
            return;

        runModule(null, selectedPeakLists, parameters.clone(), null);

    }

    public void taskStarted(Task task) {
        logger.info("Running retention time normalizer");
    }

    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {
            logger.info("Finished retention time normalizer");
        }

        if (task.getStatus() == Task.TaskStatus.ERROR) {
            String msg = "Error while normalizing peaklist: "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);
        }

    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.data.RawDataFile[],
     *      net.sf.mzmine.data.PeakList[], net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.TaskGroupListener)
     */
    public TaskGroup runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
            ParameterSet parameters, TaskGroupListener taskGroupListener) {

        // check peak lists
        if ((peakLists == null) || (peakLists.length < 2)) {
            desktop.displayErrorMessage("Please select at least 2 peak lists for normalization");
            return null;
        }

        // prepare a new group of tasks
        RTNormalizerTask task = new RTNormalizerTask(peakLists,
                (RTNormalizerParameters) parameters);
        TaskGroup newGroup = new TaskGroup(task, this, taskGroupListener);

        // start the group
        newGroup.start();

        return newGroup;

    }

    public BatchStepCategory getBatchStepCategory() {
        return BatchStepCategory.NORMALIZATION;
    }

}
