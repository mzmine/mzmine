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

package net.sf.mzmine.modules.normalization.simplestandardcompound;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ExitCode;

/**
 * 
 */
public class SimpleStandardCompoundNormalizer implements MZmineModule,
        TaskListener, ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private SimpleStandardCompoundNormalizerParameterSet parameters;

    private Desktop desktop;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        parameters = new SimpleStandardCompoundNormalizerParameterSet();

        desktop.addMenuItem(MZmineMenu.NORMALIZATION,
                "Simple standard compound normalizer", this, null,
                KeyEvent.VK_A, false, true);

    }

    public String toString() {
        return "Simple standard compoound normalizer";
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#setParameters(net.sf.mzmine.data.ParameterSet)
     */
    public ParameterSet getParameterSet() {
        return parameters;
    }

    public void setParameters(ParameterSet parameters) {
        if (parameters instanceof SimpleStandardCompoundNormalizerParameterSet)
            this.parameters = (SimpleStandardCompoundNormalizerParameterSet) parameters;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        PeakList[] selectedPeakLists = desktop.getSelectedPeakLists();
        if (selectedPeakLists.length < 1) {
            desktop.displayErrorMessage("Please select aligned peaklist");
            return;
        }

        for (PeakList pl : selectedPeakLists) {
            SimpleStandardCompoundNormalizerDialog dialog = new SimpleStandardCompoundNormalizerDialog(
                    desktop, pl, parameters);
            dialog.setVisible(true);

            if (dialog.getExitCode() != ExitCode.OK) {
                logger.info("Simple standard compound normalization cancelled.");
                return;
            }

            runModule(
                    null,
                    new PeakList[] { pl },
                    (SimpleStandardCompoundNormalizerParameterSet) parameters.clone(),
                    null);
        }

    }

    public void taskStarted(Task task) {
        logger.info("Running simple standard compound normalizer");
    }

    public void taskFinished(Task task) {

        if (task.getStatus() == Task.TaskStatus.FINISHED) {

            logger.info("Finished simple standard compound normalizer");

            PeakList normalizedPeakList = ((SimpleStandardCompoundNormalizerTask) task).getResult();

            MZmineCore.getCurrentProject().addPeakList(
                    normalizedPeakList);

        } else if (task.getStatus() == Task.TaskStatus.ERROR) {
            /* Task encountered an error */
            String msg = "Error while normalizing alignment result(s): "
                    + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);

        }

    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.io.RawDataFile[],
     *      net.sf.mzmine.data.PeakList[], net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.TaskGroupListener)
     */
    public TaskGroup runModule(RawDataFile[] dataFiles,
            PeakList[] alignmentResults,
            SimpleStandardCompoundNormalizerParameterSet parameters,
            TaskGroupListener taskGroupListener) {

        // prepare a new group of tasks

        Task tasks[] = new SimpleStandardCompoundNormalizerTask[alignmentResults.length];
        for (int i = 0; i < alignmentResults.length; i++) {
            tasks[i] = new SimpleStandardCompoundNormalizerTask(
                    alignmentResults[i], parameters);
        }
        TaskGroup newGroup = new TaskGroup(tasks, this, taskGroupListener);

        // start the group
        newGroup.start();

        return newGroup;

    }

}
