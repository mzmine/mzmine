/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking.cosine_no_precursor;


import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SignalFiltersParameters;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralSignalFilter;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.modified_cosine.ModifiedCosineSpectralNetworkingParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import org.jetbrains.annotations.NotNull;

/**
 * GC-EI-MS
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class NoPrecursorCosineSpectralNetworkingParameters extends SimpleParameterSet {

  public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter(
      ModifiedCosineSpectralNetworkingParameters.MZ_TOLERANCE.getName(),
      ModifiedCosineSpectralNetworkingParameters.MZ_TOLERANCE.getDescription(), 0.003, 10);

  public static final DoubleParameter MIN_COSINE_SIMILARITY = new DoubleParameter(
      ModifiedCosineSpectralNetworkingParameters.MIN_COSINE_SIMILARITY.getName(),
      ModifiedCosineSpectralNetworkingParameters.MIN_COSINE_SIMILARITY.getDescription(),
      MZmineCore.getConfiguration().getScoreFormat(), 0.7, 0d, 1d);


  public static final IntegerParameter MIN_MATCH = new IntegerParameter(
      ModifiedCosineSpectralNetworkingParameters.MIN_MATCH.getName(),
      "Minimum matched signals. Default is 8 for small molecules but the higher the more confident.",
      8);

  public static final ParameterSetParameter<NoPrecursorSignalFiltersParameters> signalFilters = new ParameterSetParameter<>(
      SignalFiltersParameters.NAME, SignalFiltersParameters.DESCRIPTION,
      new NoPrecursorSignalFiltersParameters());

  public NoPrecursorCosineSpectralNetworkingParameters() {
    super(
        "https://mzmine.github.io/mzmine_documentation/module_docs/group_spectral_net/molecular_networking.html",
        MZ_TOLERANCE, MIN_MATCH, MIN_COSINE_SIMILARITY, signalFilters);
  }

  public static void setAll(final ParameterSet param, final int minMatched, final double minCosine,
      final MZTolerance mzTol, final SpectralSignalFilter filter) {
    param.setParameter(MIN_MATCH, minMatched);
    param.setParameter(MIN_COSINE_SIMILARITY, minCosine);
    param.setParameter(MZ_TOLERANCE, mzTol);
    param.getParameter(signalFilters).getEmbeddedParameters().setValue(filter);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

}
