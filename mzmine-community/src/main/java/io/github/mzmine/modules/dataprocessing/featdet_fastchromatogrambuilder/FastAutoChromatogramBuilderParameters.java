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

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ChromatogramBuilderAlgorithms;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ChromatogramBuilderSettings;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Parameters of the {@link ChromatogramBuilderAlgorithms#FAST_AUTO} algorithm. The builder
 * parameters are determined from the data, see {@link BuilderParameterEstimation}.
 * <p>
 * decision: the determined values are hidden parameters, empty in the module parameters. The task
 * writes them into the parameters of the applied method, the feature list summary shows them and
 * later modules read them, see {@link #getDeterminedSettings(ParameterSet)}. A rerun determines
 * them again.
 */
public class FastAutoChromatogramBuilderParameters extends SimpleParameterSet {

  public static final ComboParameter<ChromatogramBuilderSensitivity> sensitivity = new ComboParameter<>(
      "Sensitivity", """
      The minimum height, the minimum intensity for consecutive scans, the minimum consecutive \
      scans and the m/z tolerance are determined from the noise level, the peak width and the m/z \
      scatter of the data, the values are logged and stored with the feature lists.
      Sensitive: also weak signals below the noise level and short peaks, more noise \
      chromatograms.
      Medium: signals from the noise level, peaks that reach the level of clear signals, similar \
      to the defaults of the batch wizard.
      Abundant: only intense peaks well above the noise level.""",
      ChromatogramBuilderSensitivity.values(), ChromatogramBuilderSensitivity.MEDIUM);

  private static final String DETERMINED = "Determined from the data by the applied method.";

  public static final HiddenParameter<Integer> determinedMinConsecutiveScans = new HiddenParameter<>(
      new IntegerParameter("Determined minimum consecutive scans", DETERMINED));

  public static final HiddenParameter<Double> determinedMinGroupIntensity = new HiddenParameter<>(
      new DoubleParameter("Determined minimum intensity for consecutive scans", DETERMINED,
          intensityFormat()));

  public static final HiddenParameter<Double> determinedMinHeight = new HiddenParameter<>(
      new DoubleParameter("Determined minimum absolute height", DETERMINED, intensityFormat()));

  public static final HiddenParameter<MZTolerance> determinedMzTolerance = new HiddenParameter<>(
      new MZToleranceParameter("Determined m/z tolerance (scan-to-scan)", DETERMINED));

  public static final HiddenParameter<Double> determinedNoiseLevel = new HiddenParameter<>(
      new DoubleParameter("Determined noise level", """
          Half of the data points of this intensity continue in the next scan.""",
          intensityFormat()));

  public static final HiddenParameter<Double> determinedSignalLevel = new HiddenParameter<>(
      new DoubleParameter("Determined signal level", """
          95% of the data points of this intensity continue in the next scan.""",
          intensityFormat()));

  public static final HiddenParameter<Double> determinedPeakWidthScans = new HiddenParameter<>(
      new DoubleParameter("Determined peak width (scans)", """
          Median full width at half maximum of clear peaks in scans, empty if there were too few \
          clear peaks.""", new DecimalFormat("0")));

  public FastAutoChromatogramBuilderParameters() {
    super(sensitivity, determinedMinConsecutiveScans, determinedMinGroupIntensity,
        determinedMinHeight, determinedMzTolerance, determinedNoiseLevel, determinedSignalLevel,
        determinedPeakWidthScans);
  }

  @NotNull
  private static NumberFormat intensityFormat() {
    return MZmineCore.getConfiguration().getIntensityFormat();
  }

  @NotNull
  public static FastAutoChromatogramBuilderParameters create(
      @NotNull ChromatogramBuilderSensitivity value) {
    final var param = new FastAutoChromatogramBuilderParameters().cloneParameterSet();
    param.setParameter(sensitivity, value);
    return (FastAutoChromatogramBuilderParameters) param;
  }

  /**
   * Writes the determined values, see {@link #getDeterminedSettings(ParameterSet)}.
   */
  static void setDetermined(@NotNull ParameterSet parameters,
      @NotNull BuilderParameterEstimate estimate) {
    final ChromatogramBuilderSettings settings = estimate.settings();
    parameters.setParameter(determinedMinConsecutiveScans, settings.minConsecutiveScans());
    parameters.setParameter(determinedMinGroupIntensity, settings.minGroupIntensity());
    parameters.setParameter(determinedMinHeight, settings.minHeight());
    parameters.setParameter(determinedMzTolerance, settings.mzTolerance());
    parameters.setParameter(determinedNoiseLevel,
        BuilderParameterEstimation.roundSignificant(estimate.noiseLevel()));
    parameters.setParameter(determinedSignalLevel,
        BuilderParameterEstimation.roundSignificant(estimate.signalLevel()));
    parameters.setParameter(determinedPeakWidthScans,
        Double.isFinite(estimate.peakWidthScans()) ? estimate.peakWidthScans() : null);
  }

  /**
   * @param parameters the parameters of this algorithm in an applied method
   * @return the determined builder values, empty for parameters that were not applied
   */
  @NotNull
  public static Optional<ChromatogramBuilderSettings> getDeterminedSettings(
      @NotNull ParameterSet parameters) {
    final Integer minConsecutive = parameters.getValue(determinedMinConsecutiveScans);
    final Double minGroup = parameters.getValue(determinedMinGroupIntensity);
    final Double minHeight = parameters.getValue(determinedMinHeight);
    final MZTolerance tolerance = parameters.getValue(determinedMzTolerance);
    if (minConsecutive == null || minGroup == null || minHeight == null || tolerance == null) {
      return Optional.empty();
    }
    return Optional.of(
        new ChromatogramBuilderSettings(minConsecutive, minGroup, minHeight, tolerance));
  }

  /**
   * Only the sensitivity is a user parameter, the determined values are empty before the run.
   */
  @Override
  public boolean checkParameterValues(@NotNull Collection<String> errorMessages,
      boolean skipRawDataAndFeatureListParameters) {
    return getParameter(sensitivity).checkValue(errorMessages);
  }
}
