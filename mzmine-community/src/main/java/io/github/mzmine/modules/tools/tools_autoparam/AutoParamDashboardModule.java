/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.tools_autoparam;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.AutoParamParameters;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.AutoParamTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Module that runs {@link AutoParamTask} on selected raw data files and opens the
 * {@link DataFileStatisticsDashboardPane} with the collected statistics. Accessible from the Tools
 * menu.
 */
public class AutoParamDashboardModule implements MZmineRunnableModule {

  @Override
  public @NotNull String getName() {
    return "Data file statistics";
  }

  @Override
  public @NotNull String getDescription() {
    return "Analyse raw data files to extract isotope-based statistics (FWHM, noise, tolerances) "
        + "and display them in a dashboard.";
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.TOOLS;
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return AutoParamParameters.class;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {
    final RawDataFilesSelection fileSelection = parameters.getValue(
        AutoParamParameters.RAW_DATA_FILES);
    final List<RawDataFile> files = List.of(fileSelection.getMatchingRawDataFiles());
    if (files.isEmpty()) {
      return ExitCode.ERROR;
    }

    final MemoryMapStorage storage = MemoryMapStorage.forMassList();
    tasks.add(new AutoParamDashboardTask(storage, moduleCallDate, parameters, files));
    return ExitCode.OK;
  }
}
