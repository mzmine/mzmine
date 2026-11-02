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

package io.github.mzmine.modules.dataprocessing.gapfill_gc_ei;

import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The settings to extract and confirm the signals of a row in samples with a gap.
 *
 * @param mzTolerance         tolerance to merge pseudo spectra and extract chromatograms
 * @param minConsecutiveScans minimum number of consecutive scans with signal around a peak apex
 * @param smoothingParameters parameters of the last smoothing step in the processing history, or
 *                            null if there was none
 */
public record GcEiFeatureFindingSettings(@NotNull MZTolerance mzTolerance, int minConsecutiveScans,
                                         @Nullable ParameterSet smoothingParameters) {

  /**
   * @param parameters the GC-EI gap filling parameters
   * @param methods    applied methods of the feature list, oldest first
   * @return the settings with the smoothing of the processing history, so that gap-filled
   * features are comparable to detected features
   */
  public static @NotNull GcEiFeatureFindingSettings of(@NotNull final ParameterSet parameters,
      @NotNull final List<FeatureListAppliedMethod> methods) {
    // assumption: GC-EI workflows smooth at most once, usually before resolving
    final FeatureListAppliedMethod smoothing = ParameterUtils.getLatestModuleCall(methods,
        SmoothingModule.class);
    return new GcEiFeatureFindingSettings(
        parameters.getValue(GcEiGapFillingParameters.MZ_TOLERANCE),
        parameters.getValue(GcEiGapFillingParameters.MIN_CONSECUTIVE_SCANS),
        smoothing != null ? smoothing.getParameters() : null);
  }
}
