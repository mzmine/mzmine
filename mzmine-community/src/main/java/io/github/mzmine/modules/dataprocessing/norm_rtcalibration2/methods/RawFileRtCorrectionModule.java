/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.methods;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.RTMeasure;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.RtStandard;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

/**
 * {@link MZmineModule} interface for retention time correction modules.
 */
public sealed interface RawFileRtCorrectionModule extends MZmineModule, UniqueIdSupplier permits
    MultilinearRawFileRtCorrectionModule {

  /**
   * Creates an interpolated RT correction function based on two other correction functions. The two
   * functions are weighted with the given weights.
   *
   * @param file                   The file to create the function for.
   * @param rtSortedStandards      The standards sorted by ascending retention time.
   * @param previousRunCalibration The calibration of the previous run.
   * @param previousRunWeight      The weight for the calibration of the previous run.
   * @param nextRunCalibration     The correction function of the next run
   * @param nextRunWeight          The weight for the correction function of the next run.
   * @param parameters             The parameters for this {@link RawFileRtCorrectionModule}
   * @return A new RT correction function.
   */
  AbstractRtCorrectionFunction createInterpolated(@NotNull final RawDataFile file,
      @NotNull final List<RtStandard> rtSortedStandards,
      @NotNull final AbstractRtCorrectionFunction previousRunCalibration,
      final double previousRunWeight,
      @NotNull final AbstractRtCorrectionFunction nextRunCalibration, final double nextRunWeight,
      @NotNull final RTMeasure rtMeasure, @NotNull final ParameterSet parameters);

  /**
   * Creates a new unique correction function from the standards found in the feature list/file.
   *
   * @param flist             The feature list of a single raw data file to create the calibration
   *                          function for.
   * @param rtSortedStandards The standards. Must contain the standards from the specific raw file
   * @param parameters        The parameters of this {@link RawFileRtCorrectionModule}
   * @return A new correction function.
   */
  AbstractRtCorrectionFunction createFromStandards(@NotNull final FeatureList flist,
      @NotNull final List<@NotNull RtStandard> rtSortedStandards,
      @NotNull final RTMeasure rtMeasure, @NotNull final ParameterSet parameters);

  /**
   * Loads a function from XML during project load.
   */
  AbstractRtCorrectionFunction loadFromXML(@NotNull final Element element,
      final @NotNull RawDataFilePlaceholder file);
}
