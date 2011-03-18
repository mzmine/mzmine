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

package net.sf.mzmine.modules.peaklistmethods.identification.nist;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
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
import java.io.File;

/**
 * NIST MS Search module.
 *
 * @author Chris Pudney, Syngenta Ltd
 * @version $Revision: 2369 $
 */
public class NistMsSearch implements ActionListener, BatchStep {

    // Help ID.
    private static final String HELP_ID = GUIUtils.generateHelpID(NistMsSearch.class);

    // System property holding the path to the executable.
    private static final String NIST_MS_SEARCH_PATH_PROPERTY = "nist.ms.search.path";

    // NIST MS Search home directory.
    private static final String NIST_MS_SEARCH_DIR = System.getProperty(NIST_MS_SEARCH_PATH_PROPERTY);

    // NIST MS search executable.
    private static final String SEARCH_EXE = "nistms$.exe";

    // Command-line arguments passed to executable.
    private static final String COMMAND_LINE_ARGS = "/par=2 /instrument";

    // Module name.
    private static final String MODULE_NAME = "NIST MS Search";

    // Parameters.
    private final NistMsSearchParameters parameterSet;

    /**
     * Create the module.
     */
    public NistMsSearch() {

        // Initialize parameters and add a menu item for the module (if OS is windows).
        parameterSet = new NistMsSearchParameters();
        if (System.getProperty("os.name").toUpperCase().contains("WINDOWS")) {

            MZmineCore.getDesktop().addMenuItem(MZmineMenu.IDENTIFICATION,
                                                MODULE_NAME,
                                                "Search using the NIST MS Search application",
                                                KeyEvent.VK_N, false, this, null);
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {

        // Obtain a reference to MZmine 2 desktop.
        final Desktop desktop = MZmineCore.getDesktop();

        // Obtain selected peak lists..
        final PeakList[] peakLists = desktop.getSelectedPeakLists();

        // Check that NIST MS Search directory is set.
        if (NIST_MS_SEARCH_DIR == null) {

            // Property not defined.
            desktop.displayErrorMessage("The " + NIST_MS_SEARCH_PATH_PROPERTY + " system property is not set.");

        } else if (!new File(NIST_MS_SEARCH_DIR, SEARCH_EXE).exists()) {

            // Executable missing.
            desktop.displayErrorMessage(
                    new File(NIST_MS_SEARCH_DIR, SEARCH_EXE) + " not found.  Please set the " +
                    NIST_MS_SEARCH_PATH_PROPERTY +
                    " system property to the full path of the directory containing the NIST MS Search executable.");

        } else if (peakLists.length == 0) {

            // No data file selected,
            desktop.displayErrorMessage("Please select at least one peak list.");

        } else if (parameterSet.showSetupDialog() == ExitCode.OK) {

            // Run the module.
            runModule(null, peakLists, parameterSet.clone());
        }
    }

    @Override
    public ParameterSet getParameterSet() {

        return parameterSet;
    }

    @Override
    public Task[] runModule(final RawDataFile[] dataFiles, final PeakList[] peakLists, final ParameterSet parameters) {

        // Construct the command string.
        final String searchCommand =
                new File(NIST_MS_SEARCH_DIR, SEARCH_EXE).getAbsolutePath() + ' ' + COMMAND_LINE_ARGS;

        // Process each peak list.
        final Task[] tasks = new Task[peakLists.length];
        int i = 0;
        for (final PeakList peakList : peakLists) {

            tasks[i++] = new NistMsSearchTask(peakList, NIST_MS_SEARCH_DIR, searchCommand, parameters);
        }

        // Queue and return tasks.
        MZmineCore.getTaskController().addTasks(tasks);
        return tasks;
    }

    @Override
    public BatchStepCategory getBatchStepCategory() {

        return BatchStepCategory.IDENTIFICATION;
    }

    @Override
    public String toString() {

        return MODULE_NAME;
    }
}