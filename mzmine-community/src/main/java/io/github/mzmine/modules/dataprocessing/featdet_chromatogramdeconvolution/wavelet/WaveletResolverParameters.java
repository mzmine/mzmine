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
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.FeatureResolverSetupDialog;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.Resolver;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.util.ExitCode;
import java.text.DecimalFormat;
import org.jetbrains.annotations.Nullable;

public class WaveletResolverParameters extends GeneralResolverParameters {

  public static final DoubleParameter snr = new DoubleParameter("Signal to noise threshold", "",
      new DecimalFormat("#.#"), 3d, 0d, Double.MAX_VALUE);

  public static final DoubleParameter minHeight = new DoubleParameter("Minimum height", "",
      new DecimalFormat("#.#"), 1E3, 0d, Double.MAX_VALUE);

  public static final PercentParameter boundaryThreshold = new PercentParameter(
      "Boundary detection", "", 0.05);

  public static final DoubleParameter maxScale = new DoubleParameter("Max Scale", "",
      new DecimalFormat("#.###"), 0.8, 0d, Double.MAX_VALUE);

  public static final DoubleParameter minScale = new DoubleParameter("Min Scale", "",
      new DecimalFormat("#.###"), 0.8, 0d, Double.MAX_VALUE);

  public WaveletResolverParameters() {
    super(GeneralResolverParameters.PEAK_LISTS, GeneralResolverParameters.dimension,
        GeneralResolverParameters.groupMS2Parameters, snr, boundaryThreshold, minScale, maxScale,
        minHeight,

        GeneralResolverParameters.MIN_NUMBER_OF_DATAPOINTS, GeneralResolverParameters.SUFFIX,
        GeneralResolverParameters.handleOriginal);
  }

  @Override
  public @Nullable Resolver getResolver(ParameterSet parameterSet, ModularFeatureList flist) {
//    return new WaveletResolver(new int[]{5, 10, 15, 20, 30}, parameterSet.getValue(snr),
//        parameterSet.getValue(minHeight), parameterSet.getValue(boundaryThreshold),
//        parameterSet.getValue(GeneralResolverParameters.MIN_NUMBER_OF_DATAPOINTS),
//        parameterSet.getValue(maxWidthPoints), parameterSet, flist);
    return new WaveletResolver2(parameterSet, flist);
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    final FeatureResolverSetupDialog dialog = new FeatureResolverSetupDialog(valueCheckRequired,
        this, null);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
