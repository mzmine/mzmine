/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

/**
 * 
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class IsotopePeakScannerModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Isotope peak scanner";
  private static final String MODULE_DESCRIPTION =
      "Searches for m/z and intensity distribution of a calculated isotope pattern within a feature list.";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ISOTOPES;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return IsotopePeakScannerParameters.class;
  }

  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {
    FeatureList peakLists[] = parameters.getParameter(IsotopePeakScannerParameters.PEAK_LISTS)
        .getValue().getMatchingFeatureLists();
    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();
    for (FeatureList peakList : peakLists) {
      Task newTask = new EnhancedIsotopePeakScannerTask(project, peakList, parameters, storage, moduleCallDate);
      tasks.add(newTask);
    }
    return ExitCode.OK;
  }

}
