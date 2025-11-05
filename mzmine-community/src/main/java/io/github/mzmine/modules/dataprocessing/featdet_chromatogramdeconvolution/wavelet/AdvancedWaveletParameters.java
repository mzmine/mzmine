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

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.util.StringUtils;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class AdvancedWaveletParameters extends SimpleParameterSet {

  public static final double DEFAULT_WAVELET_KERNEL = 5d;
  public static final double DEFAULT_NOISE_WINDOW = 3;
  public static final int MIN_FITTING_SCALES = 2;
  public static final @NotNull String DEFAULT_SCALES = Stream.of(1d, 1.5d, 2d, 3d, 5d, 8d, 10d)
      .map(Object::toString).collect(Collectors.joining(", "));
  public static final boolean DEFAULT_ROBUSTNESS_ITERATION = true;

  public static final OptionalParameter<StringParameter> scales = new OptionalParameter<>(
      new StringParameter("Scales", """
          Scales (widths) of the wavelets. Multiple wavelets allow detection of signals of varying widths.
          Default: %s""".formatted(DEFAULT_SCALES).formatted(), DEFAULT_SCALES));
  public static final OptionalParameter<DoubleParameter> WAVELET_KERNEL_RADIUS_FACTOR = new OptionalParameter<>(
      new DoubleParameter("Wavelet kernel radius", """
          A factor that defines the width of the wavelets. Smaller values allow detection of narrower signals.
          Default: %.0f""".formatted(DEFAULT_WAVELET_KERNEL),
          new DecimalFormat("#.###"), DEFAULT_WAVELET_KERNEL, 0d, Double.MAX_VALUE));

  public static final OptionalParameter<DoubleParameter> LOCAL_NOISE_WINDOW_FACTOR = new OptionalParameter<>(
      new DoubleParameter("Noise window factor", """
          The window around a potential signal to calculate the signal to noise ratio in.
          The window is defined by this factor multiplied by the peak width.
          Default: %.0f""".formatted(DEFAULT_NOISE_WINDOW), new DecimalFormat("#.###"),
          DEFAULT_NOISE_WINDOW));

  public static final OptionalParameter<IntegerParameter> requiredFits = new OptionalParameter<>(
      new IntegerParameter("Required fitting scales", """
          Minimum number of fitting wavelet scales for a peak to be retained.
          Default: %d""".formatted(MIN_FITTING_SCALES), MIN_FITTING_SCALES, 1, Integer.MAX_VALUE));

  public static final BooleanParameter robustnessIteration = new BooleanParameter("Apply robustness iteration", "",
      DEFAULT_ROBUSTNESS_ITERATION);

  public AdvancedWaveletParameters() {
    super(scales, WAVELET_KERNEL_RADIUS_FACTOR, LOCAL_NOISE_WINDOW_FACTOR, requiredFits, robustnessIteration);
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages,
      boolean skipRawDataAndFeatureListParameters) {
    final boolean superCheck = super.checkParameterValues(errorMessages,
        skipRawDataAndFeatureListParameters);

    try {
      final var scales = Arrays.stream(
              getEmbeddedParameterValueIfSelectedOrElse(AdvancedWaveletParameters.scales,
                  AdvancedWaveletParameters.DEFAULT_SCALES).split(",")).map(String::trim)
          .filter(s -> !StringUtils.isBlank(s)).mapToDouble(Double::valueOf).toArray();
      if (scales.length < 1) {
        errorMessages.add("No scales specified.");
        return false;
      }
    } catch (NumberFormatException | NullPointerException e) {
      errorMessages.add("Invalid scale values. Please use comma separated numbers.");
      return false;
    }
    return superCheck;
  }
}
