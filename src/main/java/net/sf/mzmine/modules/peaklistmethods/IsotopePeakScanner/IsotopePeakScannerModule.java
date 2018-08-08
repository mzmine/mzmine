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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.IsotopePeakScanner;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.peaklistmethods.identification.formulapredictionpeaklist.FormulaPredictionPeakListParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.formulapredictionpeaklist.FormulaPredictionPeakListTask;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

public class IsotopePeakScannerModule implements MZmineProcessingModule {

    private static final String MODULE_NAME = "Isotope Peak Scanner";
    private static final String MODULE_DESCRIPTION = "Compares a peak list with an isotope pattern.";

    @Override
    public @Nonnull String getName() {
        return MODULE_NAME;
    }

    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.IDENTIFICATION;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return IsotopePeakScannerParameters.class;
    }

    public @Nonnull String getDescription() {
        return MODULE_DESCRIPTION;
    }

    @Override
    public @Nonnull ExitCode runModule(@Nonnull MZmineProject project,
            @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks) {
        PeakList peakLists[] = parameters
                .getParameter(IsotopePeakScannerParameters.PEAK_LISTS)
                .getValue().getMatchingPeakLists();

        for (PeakList peakList : peakLists) {
            Task newTask = new IsotopePeakScannerTask(project, peakList,
                    parameters);
            tasks.add(newTask);
        }
        return ExitCode.OK;
    }

}
