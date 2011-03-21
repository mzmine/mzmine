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

package net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection;

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

/**
 * Baseline correction module.
 *
 * @author $Author$
 * @version $Revision$
 */
public class BaselineCorrection implements ActionListener, BatchStep {

    // Help ID.
    private static final String HELP_ID = GUIUtils.generateHelpID(BaselineCorrection.class);

    // Module name.
    private static final String MODULE_NAME = "Baseline correction";

    // Parameters.
    private final BaselineCorrectionParameters parameterSet;

    /**
     * Create the module.
     */
    public BaselineCorrection() {

        // Initialize parameters and add a menu item for the module.
        parameterSet = new BaselineCorrectionParameters();
        MZmineCore.getDesktop().addMenuItem(MZmineMenu.RAWDATAFILTERING,
                                            MODULE_NAME,
                                            "Compensates for baseline drift in ion chromatograms",
                                            KeyEvent.VK_B, false, this, null);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {

        // Obtain a reference to MZmine 2 desktop.
        final Desktop desktop = MZmineCore.getDesktop();

        // Obtain selected raw data files.
        final RawDataFile[] dataFiles = desktop.getSelectedDataFiles();
        if (dataFiles.length == 0) {

            // No data file selected,
            desktop.displayErrorMessage("Please select at least one data file.");

        } else if (parameterSet.showSetupDialog() == ExitCode.OK) {

            // Run the module.
            runModule(dataFiles, null, parameterSet.clone());
        }
    }

    @Override
    public ParameterSet getParameterSet() {

        return parameterSet;
    }

    @Override
    public String toString() {

        return MODULE_NAME;
    }

    @Override
    public Task[] runModule(final RawDataFile[] dataFiles, final PeakList[] peakLists, final ParameterSet parameters) {

        // Create a task for each data file.
        final Task[] tasks = new BaselineCorrectionTask[dataFiles.length];
        int i = 0;
        for (final RawDataFile dataFile : dataFiles) {

            tasks[i++] = new BaselineCorrectionTask(dataFile, parameters);
        }

        // Queue and return tasks.
        MZmineCore.getTaskController().addTasks(tasks);
        return tasks;
    }

    @Override
    public BatchStepCategory getBatchStepCategory() {

        return BatchStepCategory.RAWDATAPROCESSING;
    }
}
