/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.ims;

/**
 * ims plot module
 *
 * @author Nakul Bharti (knakul853@gmail.com)
 *//*
public class ImsVisualizerModule implements MZmineRunnableModule {
    private static final String MODULE_NAME = "IMS visualizer";
    private static final String MODULE_DESCRIPTION = "IMS visualizer";

    @NotNull
    @Override
    public String getDescription() {
        return MODULE_DESCRIPTION;
    }

    @NotNull
    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @NotNull
    @Override
    public ExitCode runModule(
            @NotNull MZmineProject project,
            @NotNull ParameterSet parameters,
            @NotNull Collection<Task> tasks) {

        Task newTask = new ImsVisualizerTask(parameters);
        tasks.add(newTask);
        return ExitCode.OK;
    }

    @NotNull
    @Override
    public MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.VISUALIZATIONRAWDATA;
    }

    @Nullable
    @Override
    public Class<? extends ParameterSet> getParameterSetClass() {
        return ImsVisualizerParameters.class;
    }
}
*/
