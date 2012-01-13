/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.dbsearch;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.dialogs.ExitCode;

/**
 * Module for identifying peaks by searching on-line databases.
 */
public class OnlineDBSearchModule implements MZmineProcessingModule {

    private static final String MODULE_NAME = "Online database search";

    private static OnlineDBSearchModule myInstance;

    private final ParameterSet parameters = new PeakListIdentificationParameters();

    /**
     * Create the module.
     */
    public OnlineDBSearchModule() {

        myInstance = this;
    }

    public static OnlineDBSearchModule getInstance() {

        return myInstance;
    }

    @Override
    public ParameterSet getParameterSet() {

        return parameters;
    }

    @Override
    public MZmineModuleCategory getModuleCategory() {

        return MZmineModuleCategory.IDENTIFICATION;
    }

    @Override
    public Task[] runModule(final ParameterSet set) {

        // Create a task for each peak list.
        final PeakList[] peakLists = set.getParameter(PeakListIdentificationParameters.peakLists).getValue();
        final Task[] tasks = new PeakListIdentificationTask[peakLists.length];
        int i = 0;
        for (final PeakList list : peakLists) {

            tasks[i++] = new PeakListIdentificationTask(set, list);
        }

        // Run the tasks.
        MZmineCore.getTaskController().addTasks(tasks);
        return tasks;
    }

    public String toString() {

        return MODULE_NAME;
    }

    /**
     * Show dialog for identifying a single peak-list row.
     *
     * @param row the peak list row.
     */
    public static void showSingleRowIdentificationDialog(final PeakListRow row) {

        final ParameterSet parameters = new SingleRowIdentificationParameters();

        // Set m/z.
        parameters.getParameter(SingleRowIdentificationParameters.NEUTRAL_MASS).setIonMass(row.getAverageMZ());

        // Set charge.
        final int charge = row.getBestPeak().getCharge();
        if (charge > 0) {

            parameters.getParameter(SingleRowIdentificationParameters.NEUTRAL_MASS).setCharge(charge);
        }

        // Run task.
        if (parameters.showSetupDialog() == ExitCode.OK) {

            MZmineCore.getTaskController().addTask(new SingleRowIdentificationTask(parameters.clone(), row));
        }
    }
}
