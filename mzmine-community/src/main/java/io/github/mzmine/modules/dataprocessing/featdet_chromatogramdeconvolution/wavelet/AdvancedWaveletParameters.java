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
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.StringUtils;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AdvancedWaveletParameters extends SimpleParameterSet {

  public static final double DEFAULT_WAVELET_KERNEL = 5d;
  public static final double DEFAULT_NOISE_WINDOW = 3;
  public static final int DEFAULT_MIN_FITTING_SCALES = 2;
  public static final boolean DEFAULT_ROBUSTNESS_ITERATION = true;
  public static final int DEFAULT_SIGN_CHANGE_POINTS = 2;
  public static final @NotNull String DEFAULT_SCALES = Stream.of(0.5d, 1d, 1.5d, 2d, 3d, 5d, 8d, 10d)
      .map(Object::toString).collect(Collectors.joining(", "));
  public static final double DEFAULT_SIM_HEIGHT_RATIO = 0.1d;
  public static final EdgeDetectors DEFAULT_EDGE_DETECTOR = EdgeDetectors.ABS_MIN;
  public static final boolean DEFAULT_SAT_FILTER = true;

  public static final OptionalParameter<StringParameter> scales = new OptionalParameter<>(
      new StringParameter("Scales", """
          Scales (widths) of the wavelets. Multiple wavelets allow detection of signals of varying widths.
          
          If disabled/default: %s""".formatted(DEFAULT_SCALES).formatted(), DEFAULT_SCALES));

  public static final OptionalParameter<DoubleParameter> LOCAL_NOISE_WINDOW_FACTOR = new OptionalParameter<>(
      new DoubleParameter("Noise window factor", """
          The window around a potential signal to calculate the signal to noise ratio in.
          The window is defined by this factor multiplied by the peak width.
          
          If disabled/default: %.0f is used""".formatted(DEFAULT_NOISE_WINDOW),
          new DecimalFormat("#.###"), DEFAULT_NOISE_WINDOW));

  public static final OptionalParameter<IntegerParameter> requiredFits = new OptionalParameter<>(
      new IntegerParameter("Required fitting scales", """
          Minimum number of fitting wavelet scales for a peak to be retained.
          
          If disabled/default: %d
          Note: Enable and set to 1 to disable this filter""".formatted(DEFAULT_MIN_FITTING_SCALES),
          DEFAULT_MIN_FITTING_SCALES, 1, Integer.MAX_VALUE));

  public static final BooleanParameter robustnessIteration = new BooleanParameter(
      "Apply robustness iteration", """
      Regions of potential peaks are not used for noise calculation. This may cause too many regions being excluded.
      Applying a second iteration allows more robust noise calculation for signals in crowded regions of the chromatogram.
      
      Default: %s""".formatted(DEFAULT_ROBUSTNESS_ITERATION), DEFAULT_ROBUSTNESS_ITERATION);

  public static final OptionalParameter<ComboParameter<EdgeDetectors>> edgeDetector = new OptionalParameter<>(
      new ComboParameter<>("Edge detection", """
          Define an algorithm to detect the edges of a peak.
          %s
          
          If disabled/default: %s is used""".formatted(EdgeDetectors.getDescriptions(),
          DEFAULT_EDGE_DETECTOR), EdgeDetectors.values(), DEFAULT_EDGE_DETECTOR));

  public static final OptionalParameter<IntegerParameter> signChanges = new OptionalParameter<>(
      new IntegerParameter("Allow sign changes every N points", """
          Removes noisy signals with lots of up/down movement.
          
          If disabled: %d is used.
          To not use this filter enable and set to 1.""".formatted(DEFAULT_SIGN_CHANGE_POINTS),
          DEFAULT_SIGN_CHANGE_POINTS, 1, Integer.MAX_VALUE), true);

  public static final OptionalParameter<DoubleParameter> maxSimilarHeightRatio = new OptionalParameter<>(
      new DoubleParameter("Maximum ratio of similar height signals in background", """
          Noisy baselines may pass the SNR filter due to low standard deviation if the baseline is relatively stable.
          This filter checks if there are a lot of signals with a similar absolute height (80%%) as a potential peak.
          If that is the case, the peak is removed.
          
          If disabled: (%.2f) will be used as default.
          Note: A value of 1 disables this filter.
          """.formatted(DEFAULT_SIM_HEIGHT_RATIO), new DecimalFormat("#.##"),
          DEFAULT_SIM_HEIGHT_RATIO, 0d, 1d), true);

  public static final BooleanParameter saturationFilter = new BooleanParameter("Saturation filter",
      """
          Detects if a peak has a flat top signal due to saturation of the MS detector.
          Default: %s""".formatted(DEFAULT_SAT_FILTER), DEFAULT_SAT_FILTER);

  public AdvancedWaveletParameters() {
    super(scales, LOCAL_NOISE_WINDOW_FACTOR, requiredFits, robustnessIteration, edgeDetector,
        signChanges, maxSimilarHeightRatio, saturationFilter);
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
          .filter(s -> !StringUtils.isBlank(s)).map(ParsingUtils::stringToDouble)
          .filter(Objects::nonNull).mapToDouble(Double::doubleValue).sorted().toArray();
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

  public static AdvancedWaveletParameters create(@Nullable String scales,
      @Nullable Double noiseWindowFactor, @Nullable Integer requiredFits,
      @Nullable Boolean robustnessIteration, @Nullable EdgeDetectors edgeDetection,
      @Nullable Integer signChangesEveryNPoints, @Nullable Double maxSimilarHeightRatio,
      @Nullable Boolean saturationFilter) {

    final AdvancedWaveletParameters param = (AdvancedWaveletParameters) new AdvancedWaveletParameters().cloneParameterSet();

    param.setParameter(AdvancedWaveletParameters.scales, scales != null,
        Objects.requireNonNullElse(scales, DEFAULT_SCALES));
    param.setParameter(AdvancedWaveletParameters.LOCAL_NOISE_WINDOW_FACTOR,
        noiseWindowFactor != null,
        Objects.requireNonNullElse(noiseWindowFactor, DEFAULT_NOISE_WINDOW));
    param.setParameter(AdvancedWaveletParameters.requiredFits, requiredFits != null,
        Objects.requireNonNullElse(requiredFits, DEFAULT_MIN_FITTING_SCALES));
    param.setParameter(AdvancedWaveletParameters.robustnessIteration,
        Objects.requireNonNullElse(robustnessIteration, DEFAULT_ROBUSTNESS_ITERATION));
    param.setParameter(AdvancedWaveletParameters.edgeDetector, edgeDetection != null,
        Objects.requireNonNullElse(edgeDetection, DEFAULT_EDGE_DETECTOR));
    param.setParameter(AdvancedWaveletParameters.signChanges, signChangesEveryNPoints != null,
        Objects.requireNonNullElse(signChangesEveryNPoints, DEFAULT_SIGN_CHANGE_POINTS));
    param.setParameter(AdvancedWaveletParameters.maxSimilarHeightRatio,
        maxSimilarHeightRatio != null,
        Objects.requireNonNullElse(maxSimilarHeightRatio, DEFAULT_SIM_HEIGHT_RATIO));
    param.setParameter(AdvancedWaveletParameters.saturationFilter,
        Objects.requireNonNullElse(saturationFilter, DEFAULT_SAT_FILTER));

    return param;
  }

  public static AdvancedWaveletParameters createLcDefault() {
    return create(DEFAULT_SCALES, DEFAULT_NOISE_WINDOW, DEFAULT_MIN_FITTING_SCALES,
        DEFAULT_ROBUSTNESS_ITERATION, DEFAULT_EDGE_DETECTOR, DEFAULT_SIGN_CHANGE_POINTS,
        DEFAULT_SIM_HEIGHT_RATIO, DEFAULT_SAT_FILTER);
  }

  public static AdvancedWaveletParameters createGcDefault() {
    return create(DEFAULT_SCALES, DEFAULT_NOISE_WINDOW, DEFAULT_MIN_FITTING_SCALES,
        DEFAULT_ROBUSTNESS_ITERATION, DEFAULT_EDGE_DETECTOR, DEFAULT_SIGN_CHANGE_POINTS,
        DEFAULT_SIM_HEIGHT_RATIO, DEFAULT_SAT_FILTER);
  }
}
