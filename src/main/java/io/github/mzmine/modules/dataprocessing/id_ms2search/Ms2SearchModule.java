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

package io.github.mzmine.modules.dataprocessing.id_ms2search;

import java.util.Collection;

import javax.annotation.Nonnull;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.impl.SimplePeakList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

public class Ms2SearchModule implements MZmineProcessingModule {

    private static final String MODULE_NAME = "MS2 similarity search";
    private static final String MODULE_DESCRIPTION = "This method searches for similar MS2 fragmentation spectra between two peaklists";

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

        PeakList peakList1[] = parameters
                .getParameter(Ms2SearchParameters.peakList1).getValue()
                .getMatchingPeakLists();

        PeakList peakList2[] = parameters
                .getParameter(Ms2SearchParameters.peakList2).getValue()
                .getMatchingPeakLists();

        // Previously iterated over all the peaklists & did a separate task for
        // each.
        // Now a single task.
        Task newTask = new Ms2SearchTask(parameters, peakList1[0],
                peakList2[0]);
        tasks.add(newTask);

        return ExitCode.OK;
    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.IDENTIFICATION;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return Ms2SearchParameters.class;
    }

}
