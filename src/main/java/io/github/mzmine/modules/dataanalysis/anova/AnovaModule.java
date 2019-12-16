/*
 * Copyright (C) 2018 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package io.github.mzmine.modules.dataanalysis.anova;

import java.util.*;

import javax.annotation.Nonnull;

import io.github.mzmine.datamodel.*;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

public class AnovaModule implements MZmineProcessingModule {

    private static final String MODULE_NAME = "One-way ANOVA Test";
    private static final String MODULE_DESCRIPTION = "Calculates one-way ANOVA test on the intensities of aligned features.";

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
                .getParameter(AnovaParameters.peakLists).getValue()
                .getMatchingPeakLists();

        for (PeakList peakList : peakLists) {
            tasks.add(new AnovaTask(peakList.getRows(), parameters));
        }

        return ExitCode.OK;

    }

    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.DATAANALYSIS;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return AnovaParameters.class;
    }
}
