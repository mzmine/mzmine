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

package net.sf.mzmine.modules.peakpicking.ftmsfilter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStepPeakPicking;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.userinterface.Desktop;
import net.sf.mzmine.userinterface.Desktop.MZmineMenu;
import net.sf.mzmine.userinterface.dialogs.ExitCode;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;

/**
 * FTMS shoulder peaks filter
 * 
 * Fourier-transform mass spectrometers produce continuous spectra with side
 * peaks (called shoulder peaks) on both sides of each m/z peak. Intensity of
 * these peaks is usually less than 5% of the main (real) peak. This filter can
 * remove such interference peaks from the peak list.
 * 
 */

public class FTMSFilter implements BatchStepPeakPicking, TaskListener,
        ActionListener {

    private FTMSFilterParameters parameters;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private Desktop desktop;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        parameters = new FTMSFilterParameters();

        desktop.addMenuItem(MZmineMenu.PEAKPICKING,
                "FTMS shoulder peak filter", this, null, KeyEvent.VK_F, false,
                true);

    }

    public void setParameters(ParameterSet parameters) {
        this.parameters = (FTMSFilterParameters) parameters;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        PeakList[] peaklists = desktop.getSelectedPeakLists();

        if (peaklists.length == 0) {
            desktop.displayErrorMessage("Please select peak lists to filter");
            return;
        }

        for (PeakList peaklist : peaklists) {
            if (peaklist.getNumberOfRawDataFiles() > 1) {
                desktop.displayErrorMessage("Peak list "
                        + peaklist
                        + " cannot be filtered, because it contains more than one data file");
                return;
            }
        }

        ExitCode exitCode = setupParameters(parameters);
        if (exitCode != ExitCode.OK)
            return;

        runModule(null, peaklists, parameters.clone(), null);
    }

    public void taskStarted(Task task) {
        FTMSFilterTask ftTask = (FTMSFilterTask) task;
        logger.info("Running FTMS shoulder peak filter on "
                + ftTask.getPeakList());
    }

    public void taskFinished(Task task) {

        FTMSFilterTask ftTask = (FTMSFilterTask) task;

        if (task.getStatus() == Task.TaskStatus.FINISHED) {
            logger.info("Finished FTMS shoulder peak filter on "
                    + ftTask.getPeakList());
        }

        if (task.getStatus() == Task.TaskStatus.ERROR) {
            String msg = "Error while filtering peaklist "
                    + ftTask.getPeakList() + ": " + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);
        }

    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#toString()
     */
    public String toString() {
        return "FTMS shoulder peak filter";
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
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        return parameters;
    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.io.RawDataFile[],
     *      net.sf.mzmine.data.PeakList[], net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.TaskGroupListener)
     */
    public TaskGroup runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
            ParameterSet parameters, TaskGroupListener taskGroupListener) {

        // check peak lists
        if ((peakLists == null) || (peakLists.length == 0)) {
            desktop.displayErrorMessage("Please select peak lists for filtering");
            return null;
        }

        // prepare a new group of tasks
        Task tasks[] = new FTMSFilterTask[peakLists.length];
        for (int i = 0; i < peakLists.length; i++) {
            tasks[i] = new FTMSFilterTask(peakLists[i],
                    (FTMSFilterParameters) parameters);
        }

        TaskGroup newGroup = new TaskGroup(tasks, this, taskGroupListener);

        // start the group
        newGroup.start();

        return newGroup;

    }

}
