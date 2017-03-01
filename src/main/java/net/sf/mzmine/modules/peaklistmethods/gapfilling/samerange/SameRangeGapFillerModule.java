/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.gapfilling.samerange;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

public class SameRangeGapFillerModule implements MZmineProcessingModule {

    public static final String MODULE_NAME = "Same RT and m/z range gap filler";
    private static final String MODULE_DESCRIPTION = "This method fills the missing peaks (gaps) in the peak list by looking at the whole m/z and retention time range of the peak list row and adding all raw data points in the same range.";

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

        PeakList[] peakLists = parameters
                .getParameter(SameRangeGapFillerParameters.peakLists)
                .getValue().getMatchingPeakLists();

        for (PeakList peakList : peakLists) {
            Task newTask = new SameRangeTask(project, peakList, parameters);
            tasks.add(newTask);
        }

        return ExitCode.OK;

    }

    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.GAPFILLING;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return SameRangeGapFillerParameters.class;
    }

}