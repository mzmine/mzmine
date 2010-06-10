/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.filtering.duplicatefilter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

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
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * Duplicate peak filter
 * 
 * This filter cleans up a peak list by keeping only the row with the strongest
 * median peak area of all rows having same (optionally) identification and
 * similar m/z and rt values (within tolerances)
 * 
 * Idea is to run this filter before alignment on peak lists with peaks from a
 * single raw data file in each list, but it will work on aligned peak lists
 * too.
 * 
 */

public class DuplicateFilter implements BatchStep, ActionListener {

    private DuplicateFilterParameters parameters;


    private Desktop desktop;


    final String helpID = GUIUtils.generateHelpID(this);

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        parameters = new DuplicateFilterParameters();

        desktop.addMenuItem(MZmineMenu.PEAKLISTFILTERING, toString(),
                "Removing of duplicate entries in a peak list", KeyEvent.VK_D,
                false, this, null);

    }

    public void setParameters(ParameterSet parameters) {
        this.parameters = (DuplicateFilterParameters) parameters;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        PeakList[] peakLists = desktop.getSelectedPeakLists();

        if (peakLists.length == 0) {
            desktop.displayErrorMessage("Please select peak lists to filter");
            return;
        }

        ExitCode exitCode = setupParameters(parameters);
        if (exitCode != ExitCode.OK)
            return;

        runModule(null, peakLists, parameters.clone());
    }

  

    /**
     * @see net.sf.mzmine.modules.BatchStep#toString()
     */
    public String toString() {
        return "Duplicate peak filter";
    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#setupParameters(net.sf.mzmine.data.ParameterSet)
     */
    public ExitCode setupParameters(ParameterSet currentParameters) {
        ParameterSetupDialog dialog = new ParameterSetupDialog(
                "Please set parameter values for " + toString(),
                (SimpleParameterSet) currentParameters, helpID);
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
     * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.data.RawDataFile[],
     *      net.sf.mzmine.data.PeakList[], net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.Task[]Listener)
     */
    public Task[] runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
            ParameterSet parameters) {

        // check peak lists
        if ((peakLists == null) || (peakLists.length == 0)) {
            desktop.displayErrorMessage("Please select peak lists for filtering");
            return null;
        }

        // prepare a new group of tasks
        Task tasks[] = new DuplicateFilterTask[peakLists.length];
        for (int i = 0; i < peakLists.length; i++) {
            tasks[i] = new DuplicateFilterTask(peakLists[i],
                    (DuplicateFilterParameters) parameters);
        }
        
        MZmineCore.getTaskController().addTasks(tasks);

        return tasks;

    }

    public BatchStepCategory getBatchStepCategory() {
        return BatchStepCategory.PEAKLISTPROCESSING;
    }
}
