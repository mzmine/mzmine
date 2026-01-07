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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking.cosine_no_precursor;


import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SignalFiltersParameters;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralSignalFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;

/**
 * Filters to apply to spectra
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class NoPrecursorSignalFiltersParameters extends SimpleParameterSet {

  public static final IntegerParameter signalThresholdIntensityFilter = new IntegerParameter(
      SignalFiltersParameters.signalThresholdIntensityFilter.getName(),
      SignalFiltersParameters.signalThresholdIntensityFilter.getDescription(), 100, 0, null);


  public static final PercentParameter intensityPercentFilter = new PercentParameter(
      SignalFiltersParameters.intensityPercentFilter.getName(),
      SignalFiltersParameters.intensityPercentFilter.getDescription(), 0.98, 0.1, 1d);

  public static final IntegerParameter cropToMaxSignals = new IntegerParameter(
      SignalFiltersParameters.cropToMaxSignals.getName(),
      SignalFiltersParameters.cropToMaxSignals.getDescription(), 350, 2, null);


  public NoPrecursorSignalFiltersParameters() {
    super(cropToMaxSignals, signalThresholdIntensityFilter, intensityPercentFilter);
  }

  public SpectralSignalFilter createFilter() {
    return createFilter(this);
  }

  public static SpectralSignalFilter createFilter(ParameterSet params) {
    var cropToMaxSignals = params.getValue(NoPrecursorSignalFiltersParameters.cropToMaxSignals);
    var signalThresholdForTargetIntensityPercent = params.getValue(signalThresholdIntensityFilter);
    var targetIntensityPercentage = params.getValue(intensityPercentFilter);
    return new SpectralSignalFilter(cropToMaxSignals, signalThresholdForTargetIntensityPercent,
        targetIntensityPercentage);
  }

  public NoPrecursorSignalFiltersParameters setValue(SpectralSignalFilter filter) {
    setParameter(intensityPercentFilter, filter.targetIntensityPercentage());
    setParameter(signalThresholdIntensityFilter, filter.signalThresholdForTargetIntensityPercent());
    setParameter(cropToMaxSignals, filter.cropToMaxSignals());
    return this;
  }
}
