/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_spectraldbsearch;

import java.util.Collection;
import javax.annotation.Nonnull;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.visualization.featurelisttable.table.PeakListTable;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

public class LocalSpectralDBSearchModule implements MZmineProcessingModule {

    public static final String MODULE_NAME = "Local spectra database search";
    private static final String MODULE_DESCRIPTION = "This method searches all peaklist rows against a local spectral database.";

    @Override
    public @Nonnull String getName() {
        return MODULE_NAME;
    }

    @Override
    public @Nonnull String getDescription() {
        return MODULE_DESCRIPTION;
    }

    @Override
    @Nonnull
    public ExitCode runModule(@Nonnull MZmineProject project,
            @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks) {

        PeakList peakLists[] = parameters
                .getParameter(LocalSpectralDBSearchParameters.peakLists)
                .getValue().getMatchingPeakLists();

        for (PeakList peakList : peakLists) {
            Task newTask = new LocalSpectralDBSearchTask(peakList, parameters);
            tasks.add(newTask);
        }

        return ExitCode.OK;

    }

    /**
     * Show dialog for identifying multiple selected peak-list rows.
     * 
     * @param row
     *            the feature list row.
     */
    public static void showSelectedRowsIdentificationDialog(
            final PeakListRow[] rows, PeakListTable table) {

        final ParameterSet parameters = new SelectedRowsLocalSpectralDBSearchParameters();

        if (parameters.showSetupDialog(MZmineCore.getDesktop().getMainWindow(),
                true) == ExitCode.OK) {

            MZmineCore.getTaskController()
                    .addTask(new SelectedRowsLocalSpectralDBSearchTask(rows,
                            table, parameters.cloneParameterSet()));
        }
    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.IDENTIFICATION;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return LocalSpectralDBSearchParameters.class;
    }

}
