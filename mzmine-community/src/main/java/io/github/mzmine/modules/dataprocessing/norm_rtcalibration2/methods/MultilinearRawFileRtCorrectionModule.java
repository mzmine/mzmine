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
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.RTMeasure;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.RtStandard;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import io.github.mzmine.util.ParsingUtils;
import java.util.List;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public final class MultilinearRawFileRtCorrectionModule implements RawFileRtCorrectionModule {

  @Override
  public AbstractRtCorrectionFunction createInterpolated(@NotNull RawDataFile file,
      @NotNull List<RtStandard> rtSortedStandards,
      @NotNull AbstractRtCorrectionFunction previousRunCalibration, double previousRunWeight,
      @NotNull AbstractRtCorrectionFunction nextRunCalibration, double nextRunWeight,
      @NotNull final RTMeasure rtMeasure, @NotNull ParameterSet parameters) {

    return new MultiLinearRtCorrectionFunction(file, rtSortedStandards,
        (MultiLinearRtCorrectionFunction) previousRunCalibration, previousRunWeight,
        (MultiLinearRtCorrectionFunction) nextRunCalibration, nextRunWeight,
        parameters.getValue(MultilinearRawFileRtCalibrationParameters.correctionBandwidth),
        rtMeasure);
  }

  @Override
  public AbstractRtCorrectionFunction createFromStandards(@NotNull FeatureList flist,
      @NotNull List<@NotNull RtStandard> rtSortedStandards, @NotNull final RTMeasure rtMeasure,
      @NotNull ParameterSet parameters) {
    return new MultiLinearRtCorrectionFunction(flist, rtSortedStandards,
        parameters.getValue(MultilinearRawFileRtCalibrationParameters.correctionBandwidth),
        rtMeasure);
  }

  @Override
  public AbstractRtCorrectionFunction loadFromXML(@NotNull Element element,
      final @NotNull RawDataFilePlaceholder file) {

    final PolynomialSplineFunction polynomialSplineFunction = ParsingUtils.loadSplineFunctionFromParentXmlElement(
        element);
    return new MultiLinearRtCorrectionFunction(file, polynomialSplineFunction);
  }

  @Override
  public @NotNull String getUniqueID() {
    return "multi_linear_raw_file_rt_calibration";
  }

  @Override
  public @NotNull String getName() {
    return "Multi linear RT calibration";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return MultilinearRawFileRtCalibrationParameters.class;
  }
}
