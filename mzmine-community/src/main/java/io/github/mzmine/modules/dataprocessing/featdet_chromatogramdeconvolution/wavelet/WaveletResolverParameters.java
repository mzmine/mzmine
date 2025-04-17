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
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverSetupDialog;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.Resolver;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.ListDoubleParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.util.ExitCode;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public class WaveletResolverParameters extends GeneralResolverParameters {

  private static final double mergeProximity = 0.1;

  public static final DoubleParameter snr = new DoubleParameter("Signal to noise threshold", "",
      new DecimalFormat("#.#"), 3d, 0d, Double.MAX_VALUE);

  public static final DoubleParameter minHeight = new DoubleParameter("Minimum height", "",
      ConfigService.getGuiFormats().intensityFormat(), 1E3, 0d, Double.MAX_VALUE);

  public static final DoubleParameter LOCAL_NOISE_WINDOW_FACTOR = new DoubleParameter(
      "LOCAL_NOISE_WINDOW_FACTOR", "", new DecimalFormat("#.###"), 3.0);

  public static final DoubleParameter WAVELET_KERNEL_RADIUS_FACTOR = new DoubleParameter(
      "WAVELET_KERNEL_RADIUS_FACTOR", "", new DecimalFormat("#.###"), 1d, 0d, Double.MAX_VALUE);

  public static final StringParameter scales = new StringParameter("Scales", "",
      Stream.of(1d, 2d, 3d, 4d, 5d, 6d, 8d, 10d).map(Object::toString)
          .collect(Collectors.joining(", ")));

  private static final BooleanParameter old = new BooleanParameter("old", "", true);

  public WaveletResolverParameters() {
    super(GeneralResolverParameters.PEAK_LISTS, GeneralResolverParameters.dimension,
        GeneralResolverParameters.groupMS2Parameters, snr, WAVELET_KERNEL_RADIUS_FACTOR, minHeight,
        LOCAL_NOISE_WINDOW_FACTOR, scales, old,

        GeneralResolverParameters.MIN_NUMBER_OF_DATAPOINTS, GeneralResolverParameters.SUFFIX,
        GeneralResolverParameters.handleOriginal);
  }

  @Override
  public @Nullable Resolver getResolver(ParameterSet parameterSet, ModularFeatureList flist) {

    var scales = Arrays.stream(parameterSet.getValue(WaveletResolverParameters.scales).split(","))
        .map(String::trim).mapToDouble(Double::valueOf).toArray();

    if(parameterSet.getValue(WaveletResolverParameters.old)) {
      return new WaveletPeakDetector2(scales, parameterSet.getValue(snr),
          parameterSet.getValue(minHeight), mergeProximity,
          parameterSet.getValue(WAVELET_KERNEL_RADIUS_FACTOR),
          parameterSet.getValue(LOCAL_NOISE_WINDOW_FACTOR).intValue(), flist, parameterSet);
    }

    return new WaveletPeakDetector(scales, parameterSet.getValue(snr),
        parameterSet.getValue(minHeight), mergeProximity,
        parameterSet.getValue(WAVELET_KERNEL_RADIUS_FACTOR),
        parameterSet.getValue(LOCAL_NOISE_WINDOW_FACTOR).intValue(), flist, parameterSet);
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    final FeatureResolverSetupDialog dialog = new FeatureResolverSetupDialog(valueCheckRequired,
        this, null);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
