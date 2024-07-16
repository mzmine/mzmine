/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.process_fragmentsanalysis;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.TaskPerFeatureListModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.MemoryMapStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * A Module creates tasks which are then added queue
 * <p>
 * 1. add your module to the io.github.mzmine.gui.mainwindow.MainMenu.fmxl
 * <p>
 * 2. for access in batch mode, put your module in the BatchModeModulesList in the package:
 * io.github.mzmine.main
 */
public class FragmentsAnalysisModule extends TaskPerFeatureListModule {

    public FragmentsAnalysisModule() {
        super("Fragments analysis module", FragmentsAnalysisParameters.class, MZmineModuleCategory.FEATURELISTEXPORT,
                false, "This is a description of the amazing quest we have...");
    }

    @Override
    public @NotNull Task createTask(
            final @NotNull MZmineProject project,
            final @NotNull ParameterSet parameters,
            final @NotNull Instant moduleCallDate,
            final @Nullable MemoryMapStorage storage,
            final @NotNull FeatureList featureList) {
        return new FragmentsAnalysisTask(project, List.of(featureList), parameters, storage, moduleCallDate,
                this.getClass());
    }

}