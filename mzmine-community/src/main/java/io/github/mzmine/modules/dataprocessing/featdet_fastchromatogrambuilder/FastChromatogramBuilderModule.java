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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.batchmode.order.MassDetectionCondition;
import io.github.mzmine.modules.batchmode.order.ModuleOrderRecommendation;
import io.github.mzmine.modules.batchmode.order.ModuleOrderRule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Chromatogram builder that connects data points in a single loop over the scans instead of sorting
 * all data points by intensity, see {@link FastChromatogramBuilder}.
 */
public class FastChromatogramBuilderModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Fast chromatogram builder";
  private static final String MODULE_DESCRIPTION = """
      Connects data points from mass lists to chromatograms. Traces are followed through the scans \
      and grouped into one chromatogram per m/z channel.""";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  public @NotNull List<@NotNull ModuleOrderRecommendation> getModuleOrderRecommendations() {
    return List.of(ModuleOrderRecommendation.of(
        "Chromatogram building requires centroided and noise filtered data",
        ModuleOrderRule.mustRunAfter(MassDetectionCondition.MS1)));
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    // one memory map storage per module call to reduce number of files and connect related
    // feature lists
    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();

    final RawDataFile[] dataFiles = parameters.getValue(FastChromatogramBuilderParameters.dataFiles)
        .getMatchingRawDataFiles();
    // decision: one main task for all files, it estimates the tolerance once and then processes
    // the files in parallel
    tasks.add(
        new FastChromatogramBuilderTask(project, dataFiles, parameters.cloneParameterSet(true),
            storage, moduleCallDate, FastChromatogramBuilderModule.class));
    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.EIC_DETECTION;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return FastChromatogramBuilderParameters.class;
  }
}
