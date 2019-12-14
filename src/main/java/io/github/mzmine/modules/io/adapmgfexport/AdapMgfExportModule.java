/*
 * Copyright (C) 2016 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */

package io.github.mzmine.modules.io.adapmgfexport;

import java.util.Collection;
import javax.annotation.Nonnull;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */
public class AdapMgfExportModule implements MZmineProcessingModule {
    private static final String MODULE_NAME = "Export to MGF file (ADAP)";
    private static final String MODULE_DESCRIPTION = "This method exports all ADAP clustered spectra in a feature lists to an MGF file. Used in the GNPS GC-MS workflow.";

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
        AdapMgfExportTask task = new AdapMgfExportTask(parameters);
        tasks.add(task);
        return ExitCode.OK;

    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.PEAKLISTEXPORT;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return AdapMgfExportParameters.class;
    }

}
