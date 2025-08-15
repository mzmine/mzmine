/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.noiseamplitude;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverSetupDialog;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.Resolver;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.util.ExitCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NoiseAmplitudeFeatureResolverParameters extends GeneralResolverParameters {

  public static final DoubleParameter MIN_PEAK_HEIGHT = new DoubleParameter("Min peak height",
      "Minimum acceptable height (intensity) for a chromatographic peak",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final DoubleRangeParameter PEAK_DURATION =
      new DoubleRangeParameter("Peak duration range (min)", "Range of acceptable peak lengths",
          MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.0, 10.0));

  public static final DoubleParameter NOISE_AMPLITUDE = new DoubleParameter("Amplitude of noise",
      "This value is the intensity amplitude of the signal in the noise region",
      MZmineCore.getConfiguration().getIntensityFormat());

  public NoiseAmplitudeFeatureResolverParameters() {
    super(new Parameter[]{PEAK_LISTS, SUFFIX, handleOriginal, groupMS2Parameters, dimension, MIN_PEAK_HEIGHT,
        PEAK_DURATION, NOISE_AMPLITUDE, MIN_NUMBER_OF_DATAPOINTS},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_resolver_noise_ampl/noise-ampl-resolver.html");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    final FeatureResolverSetupDialog dialog =
        new FeatureResolverSetupDialog(valueCheckRequired, this, null);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public @Nullable Resolver getResolver(ParameterSet parameterSet, ModularFeatureList flist) {
    return new NoiseAmplitudeFeatureResolver(parameterSet, flist);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
