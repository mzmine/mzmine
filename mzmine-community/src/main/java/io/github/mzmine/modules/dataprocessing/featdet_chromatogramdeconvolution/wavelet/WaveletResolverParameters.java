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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverSetupDialog;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.Resolver;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersParameter;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.StringUtils;
import java.text.DecimalFormat;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WaveletResolverParameters extends GeneralResolverParameters {

  public static final DoubleParameter snr = new DoubleParameter("Signal to noise threshold", "",
      new DecimalFormat("#.#"), 3d, 1d, Double.MAX_VALUE);
  public static final OptionalParameter<DoubleParameter> topToEdge = new OptionalParameter<>(
      new DoubleParameter("Top to edge (SNR override)", "", new DecimalFormat("#.#"), 3d, 1d,
          Double.MAX_VALUE), false);
  public static final DoubleParameter minHeight = new DoubleParameter("Minimum height", "",
      ConfigService.getGuiFormats().intensityFormat(), 1E3, 0d, Double.MAX_VALUE);
  public static final ComboParameter<NoiseCalculation> noiseCalculation = new ComboParameter<>(
      "Noise calculation", "Choose a method to calculate the noise around a potential signal.",
      NoiseCalculation.values(), NoiseCalculation.STANDARD_DEVIATION);
  public static final AdvancedParametersParameter<AdvancedWaveletParameters> advancedParameters = new AdvancedParametersParameter<>(
      new AdvancedWaveletParameters());
  private static final double mergeProximity = 0.1;

  public WaveletResolverParameters() {
    super(GeneralResolverParameters.PEAK_LISTS, GeneralResolverParameters.dimension,
        GeneralResolverParameters.groupMS2Parameters, snr, topToEdge, minHeight, noiseCalculation,

        GeneralResolverParameters.MIN_NUMBER_OF_DATAPOINTS, GeneralResolverParameters.SUFFIX,
        GeneralResolverParameters.handleOriginal,

        advancedParameters);
  }

  @Override
  public @Nullable Resolver getResolver(ParameterSet parameterSet, ModularFeatureList flist) {

//    final var scales = Arrays.stream(
//            parameterSet.getValue(WaveletResolverParameters.scales).split(",")).map(String::trim)
//        .mapToDouble(Double::valueOf).toArray();

    final AdvancedParametersParameter<AdvancedWaveletParameters> advanced = parameterSet.getParameter(
        advancedParameters);
    final Double topToEdge = getEmbeddedParameterValueIfSelectedOrElse(
        WaveletResolverParameters.topToEdge, null);

    final double waveletKernel = advanced.getValueOrDefault(
        AdvancedWaveletParameters.WAVELET_KERNEL_RADIUS_FACTOR,
        AdvancedWaveletParameters.DEFAULT_WAVELET_KERNEL);
    final int noiseWindow = advanced.getValueOrDefault(
        AdvancedWaveletParameters.LOCAL_NOISE_WINDOW_FACTOR,
        AdvancedWaveletParameters.DEFAULT_NOISE_WINDOW).intValue();
    final var scales = Arrays.stream(advanced.getValueOrDefault(AdvancedWaveletParameters.scales,
            AdvancedWaveletParameters.DEFAULT_SCALES).split(",")).map(String::trim)
        .filter(s -> !StringUtils.isBlank(s)).mapToDouble(Double::valueOf).toArray();
    final int minFittingScales = advanced.getValueOrDefault(AdvancedWaveletParameters.requiredFits,
        AdvancedWaveletParameters.MIN_FITTING_SCALES);

    return new WaveletPeakDetector(scales, parameterSet.getValue(WaveletResolverParameters.snr),
        topToEdge, parameterSet.getValue(WaveletResolverParameters.minHeight), mergeProximity,
        waveletKernel, noiseWindow, minFittingScales, flist, parameterSet);
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    final FeatureResolverSetupDialog dialog = new FeatureResolverSetupDialog(valueCheckRequired,
        this, null);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  public enum NoiseCalculation implements UniqueIdSupplier {
    STANDARD_DEVIATION, MEDIAN_ABSOLUTE_DEVIATION;


    @Override
    public @NotNull String getUniqueID() {
      return switch (this) {
        case STANDARD_DEVIATION -> "standard_deviation";
        case MEDIAN_ABSOLUTE_DEVIATION -> "median_absolute_deviation";
      };
    }

    @Override
    public String toString() {
      return switch (this) {
        case STANDARD_DEVIATION -> "Standard Deviation";
        case MEDIAN_ABSOLUTE_DEVIATION -> "Median absolute deviation";
      };
    }
  }
}
