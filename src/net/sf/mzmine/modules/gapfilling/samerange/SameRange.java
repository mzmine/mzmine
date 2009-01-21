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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.gapfilling.samerange;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

public class SameRange implements BatchStep, ActionListener {

    private SameRangeParameters parameters;

    private Desktop desktop;

    /**
     * @see net.sf.mzmine.main.mzmineclient.MZmineModule#initModule(net.sf.mzmine.main.mzmineclient.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        parameters = new SameRangeParameters();

        desktop.addMenuItem(
                MZmineMenu.GAPFILLING,
                "Same RT and m/z range filler",
                "Secondary peak detection, to fill gaps in a peak list using same RT and m/z range",
                KeyEvent.VK_S, false, this, null);

    }

    public String toString() {
        return "Gap filler";
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        PeakList[] selectedPeakLists = desktop.getSelectedPeakLists();
        if (selectedPeakLists.length < 1) {
            desktop.displayErrorMessage("Please select peak lists for gap-filling");
            return;
        }

        ExitCode exitCode = setupParameters(parameters);
        if (exitCode != ExitCode.OK)
            return;

        runModule(null, selectedPeakLists, parameters.clone(), null);

    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#setupParameters(net.sf.mzmine.data.ParameterSet)
     */
    public ExitCode setupParameters(ParameterSet currentParameters) {
        ParameterSetupDialog dialog = new ParameterSetupDialog(
                "Please set parameter values for " + toString(),
                (SameRangeParameters) currentParameters);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }

    /**
     * @see net.sf.mzmine.main.mzmineclient.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        return parameters;
    }

    public void setParameters(ParameterSet parameters) {
        this.parameters = (SameRangeParameters) parameters;
    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.data.RawDataFile[],
     *      net.sf.mzmine.data.PeakList[], net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.TaskGroupListener)
     */
    public TaskGroup runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
            ParameterSet parameters, TaskGroupListener taskGroupListener) {

        if (peakLists == null || peakLists.length < 1) {
            throw new IllegalArgumentException(
                    "Gap-filling requires a peak list");
        }

        // prepare a new group of tasks
        Task tasks[] = new SameRangeTask[peakLists.length];
        for (int i = 0; i < peakLists.length; i++) {
            tasks[i] = new SameRangeTask(peakLists[i],
                    (SameRangeParameters) parameters);
        }

        TaskGroup newGroup = new TaskGroup(tasks, null, taskGroupListener);
        newGroup.start();

        return newGroup;

    }

    public BatchStepCategory getBatchStepCategory() {
        return BatchStepCategory.GAPFILLING;
    }

}