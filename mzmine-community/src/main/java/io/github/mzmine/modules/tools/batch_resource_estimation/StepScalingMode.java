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

package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingModule;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * How the working memory and time of a batch step grow with the number of files. The retained
 * data and the temp files of all steps grow with the number of files.
 */
enum StepScalingMode {
  /**
   * Tasks run in parallel on at most the configured number of threads. The working memory grows
   * with min(files, threads) and the time with the number of thread waves.
   */
  SCALES_WITH_THREADS,
  /**
   * Works across all files at once, like alignment and gap filling. Working memory and time grow
   * with all files.
   */
  SCALES_WITH_FILES;

  /**
   * @return the scaling mode of each step in the order of the queue
   */
  static @NotNull List<StepScalingMode> ofSteps(
      @NotNull final List<MZmineProcessingStep<MZmineProcessingModule>> steps) {
    return steps.stream().map(step -> of(step.getModule())).toList();
  }

  static @NotNull StepScalingMode of(@NotNull final MZmineProcessingModule module) {
    // decision: correlation grouping correlates the features of all samples per row, measured to
    // grow with the files. The other feature grouping modules keep the scaling of their category.
    if (module instanceof CorrelateGroupingModule) {
      return SCALES_WITH_FILES;
    }
    return of(module.getModuleCategory());
  }

  /**
   * decision: alignment and gap filling work across all files, all other categories are limited
   * by the number of threads. Their input and results still grow with the files, see
   * {@link InStepMemory}.
   */
  static @NotNull StepScalingMode of(@NotNull final MZmineModuleCategory category) {
    return switch (category) {
      case ALIGNMENT, GAPFILLING -> SCALES_WITH_FILES;
      case PROJECTIO, PROJECT, PROJECTMETADATA, RAWDATAIMPORT, RAWDATA, RAWDATAFILTERING,
           RAWDATAEXPORT, EIC_DETECTION, ISOTOPES, FEATURELIST, FEATURE_RESOLVING,
           FEATURE_GROUPING, ION_IDENTITY_NETWORKS, SPECTRALDECONVOLUTION, FEATURELISTFILTERING,
           NORMALIZATION, ANNOTATION, FEATURELISTEXPORT, FEATURELISTIMPORT, SPECLIBIMPORT,
           SPECLIBEXPORT, SPECLIB_PROCESSING, VISUALIZATIONRAWDATA, VISUALIZATIONFEATURELIST,
           VISUALIZATION_RAW_AND_FEATURE, VISUALIZATION_OTHER_DATA, DATAANALYSIS, HELPSYSTEM, TOOLS,
           OTHER_DATA_PROCESSING -> SCALES_WITH_THREADS;
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case SCALES_WITH_THREADS -> "threads";
      case SCALES_WITH_FILES -> "files";
    };
  }
}
