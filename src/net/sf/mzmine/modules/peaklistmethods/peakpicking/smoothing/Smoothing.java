/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.smoothing;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.ExitCode;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * Chromatographic smoothing.
 *
 * @author $Author$
 * @version $Revision$
 */
public class Smoothing implements BatchStep, ActionListener {

    // Help ID.
    private static final String HELP_ID = GUIUtils.generateHelpID(Smoothing.class);

    // Module name.
    private static final String MODULE_NAME = "Smoothing";

    // Task parameters.
    private final SmoothingParameters parameterSet;

    /**
     * Create the module.
     */
    public Smoothing() {

        // Initialize parameters and add a menu item for the module.
        parameterSet = new SmoothingParameters();
        MZmineCore.getDesktop().addMenuItem(MZmineMenu.PEAKLISTPICKING,
                                            MODULE_NAME,
                                            "Smooths peak chromatograms",
                                            KeyEvent.VK_S, false, this, null);
    }

    /**
     * Run the module.
     */
    @Override public void actionPerformed(final ActionEvent e) {

        // Check selected peak lists then show parameters dialog.
        final PeakList[] peakLists = MZmineCore.getDesktop().getSelectedPeakLists();
        if (isReadyToRun(peakLists) && parameterSet.showSetupDialog() == ExitCode.OK) {

            runModule(null, peakLists, parameterSet.clone());
        }
    }

    @Override
    public ParameterSet getParameterSet() {

        return parameterSet;
    }

    @Override public Task[] runModule(final RawDataFile[] dataFiles,
                                      final PeakList[] peakLists,
                                      final ParameterSet parameters) {

        Task[] tasks = null;
        if (isReadyToRun(peakLists)) {

            // Create a task for each peak list.
            tasks = new Task[peakLists.length];
            int i = 0;
            for (final PeakList peakList : peakLists) {

                tasks[i++] = new SmoothingTask(peakList, parameters);
            }

            // Queue and return tasks.
            MZmineCore.getTaskController().addTasks(tasks);
        }
        return tasks;
    }

    @Override public BatchStepCategory getBatchStepCategory() {

        return BatchStepCategory.PEAKLISTPROCESSING;
    }

    @Override
    public String toString() {

        return MODULE_NAME;
    }

    /**
     * Checks before running module - display error messages.
     *
     * @param peakLists the peak lists.
     * @return true/false if checks are passed/failed.
     */
    private static boolean isReadyToRun(final PeakList[] peakLists) {

        boolean ok = true;
        if (peakLists == null || peakLists.length == 0) {

            MZmineCore.getDesktop().displayErrorMessage(MODULE_NAME + ": No Peak-List Selected",
                                                        "Please select at least one peak-list");
            ok = false;
        }
        return ok;
    }
}
