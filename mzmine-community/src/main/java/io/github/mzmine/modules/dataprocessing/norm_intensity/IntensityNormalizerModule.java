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
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.function.BooleanSupplier;
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
      final Task newTask;
      if (Boolean.TRUE.equals(parameters.getValue(IntensityNormalizerParameters.hiddenReviewedFunctions))) {
        final var summary = parameters.getValue(IntensityNormalizerParameters.hiddenNormalizationSummary);
        newTask = IntensityNormalizerTask.createReplayTask(project, featureList,
            parameters.getValue(IntensityNormalizerParameters.suffix),
            parameters.getValue(IntensityNormalizerParameters.hiddenSourceRevision), summary,
            parameters.getValue(IntensityNormalizerParameters.hiddenNativeSourceFingerprint),
            parameters, IntensityNormalizerModule.class, storage, moduleCallDate);
      } else {
        newTask = new IntensityNormalizerTask(project, featureList, parameters, storage, moduleCallDate);
      }
      tasks.add(newTask);
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

  /**
   * Creates the native normalizer task for functions that were calculated from a reviewed
   * snapshot. The caller must bind every function to the source table's actual raw-data-file
   * instance before calling this method.
   */
  public static @NotNull Task createReviewedTask(final @NotNull MZmineProject project,
      final @NotNull ModularFeatureList source, final @NotNull String suffix,
      final @NotNull String sourceRevision, final @NotNull IntensityNormalizationSummary summary,
      final @NotNull MemoryMapStorage storage, final @NotNull Instant moduleCallDate,
      final @NotNull BooleanSupplier current) {
    final IntensityNormalizerParameters parameters = (IntensityNormalizerParameters) new IntensityNormalizerParameters()
        .cloneParameterSet();
    parameters.setParameter(IntensityNormalizerParameters.featureLists,
        new FeatureListsSelection(source));
    parameters.setParameter(IntensityNormalizerParameters.suffix, suffix);
    parameters.setParameter(IntensityNormalizerParameters.handleOriginal,
        OriginalFeatureListOption.KEEP);
    parameters.setParameter(IntensityNormalizerParameters.hiddenNormalizationSummary, summary);
    parameters.setParameter(IntensityNormalizerParameters.hiddenReviewedFunctions, true);
    parameters.setParameter(IntensityNormalizerParameters.hiddenSourceRevision, sourceRevision);
    return IntensityNormalizerTask.createReviewedTask(project, source, suffix, sourceRevision, summary,
        parameters, IntensityNormalizerModule.class, storage, moduleCallDate, current);
  }

  public static @NotNull IntensityNormalizationSummary getNormalizationFunctionsOfLatestCall(
      final @NotNull FeatureList featureList) {
    // decision: every intensity-normalizer call writes a complete replacement set of normalized
    // values, so the last one owns re-normalization for newly created features.
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
    IntensityNormalizationSummary summary = getNormalizationFunctionsOfLatestCall(featureList);
    return summary.functions().stream()
        .filter(func -> func.rawDataFilePlaceholder().matches(rawDataFile))
        .map(RawFileNormalizationFunction::function).findFirst();
  }

}
