/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class IntensityNormalizerModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Intensity normalizer";
  private static final String MODULE_DESCRIPTION = "Intensity normalizers scales feature intensities by a normalization factor or function determined according to the selected normalization type.";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {

    FeatureList featureLists[] = parameters.getParameter(IntensityNormalizerParameters.featureLists)
        .getValue().getMatchingFeatureLists();
    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();

    for (FeatureList featureList : featureLists) {
      tasks.add(new IntensityNormalizerTask(project, featureList, parameters, storage, moduleCallDate));
    }

    return ExitCode.OK;

  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.NORMALIZATION;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return IntensityNormalizerParameters.class;
  }

  public static @NotNull IntensityNormalizationSummary getNormalizationFunctionsOfLatestCall(
      final @NotNull FeatureList featureList) {
    // decision: every intensity-normalizer call replaces the normalization in the settings, so the
    // last one owns re-normalization for newly created features.
    final IntensityNormalizationSummary active = featureList.getSettings().normalization();
    return active == null ? IntensityNormalizationSummary.EMPTY : active;
  }

  /**
   * Reads legacy project history once while loading projects that predate directly persisted
   * normalization state. Normal runtime code must use {@link #getNormalizationFunctionsOfLatestCall}.
   */
  public static @NotNull IntensityNormalizationSummary getLegacyNormalizationFromAppliedMethods(
      final @NotNull FeatureList featureList) {
    final var methods = featureList.getAppliedMethods();
    for (int index = methods.size() - 1; index >= 0; index--) {
      final FeatureListAppliedMethod method = methods.get(index);
      if (method == null) continue;
      final IntensityNormalizationSummary summary;
      if (method.getModule() instanceof IntensityNormalizerModule) {
        summary = method.getParameters().getValue(IntensityNormalizerParameters.hiddenNormalizationSummary);
      } else {
        summary = null;
      }
      if (summary != null) return summary;
    }
    return IntensityNormalizationSummary.EMPTY;
  }

  /**
   * @return optional of {@link NormalizationFunction} to be applied to features of raw data file
   */
  public static @NotNull Optional<NormalizationFunction> getNormalizationFunctionsOfLatestCallForFile(
      final @NotNull FeatureList featureList, final @NotNull RawDataFile rawDataFile) {
    final IntensityNormalizationSummary summary = featureList.getSettings().normalization();
    if (summary == null) {
      return Optional.empty();
    }
    return summary.functions().stream()
        .filter(func -> func.rawDataFilePlaceholder().matches(rawDataFile))
        .map(RawFileNormalizationFunction::function).findFirst();
  }

}
