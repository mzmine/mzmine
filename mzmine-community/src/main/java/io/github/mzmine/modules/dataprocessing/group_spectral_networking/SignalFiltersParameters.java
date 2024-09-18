/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking;


import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;

/**
 * Filters to apply to spectra
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class SignalFiltersParameters extends SimpleParameterSet {

  public static final OptionalParameter<DoubleParameter> removePrecursor = new OptionalParameter<>(
      new DoubleParameter("Remove residual precursor m/z",
          "This is strongly recommended to remove signals around the precursor as they will always match between spectra. This should include isotope signals. The default is m/z 10, this should not include meaningful signals.",
          MZmineCore.getConfiguration().getMZFormat(), 10d, 0d, null), true);

  public static final IntegerParameter signalThresholdIntensityFilter = new IntegerParameter(
      "Signal threshold (intensity filter)", """
      Above this number of signals, the signals are filtered to include only the top N signals that make up X% of the intensity. This might include fewer than the specified number of signals.
      """, 50, 0, null);


  public static final PercentParameter intensityPercentFilter = new PercentParameter(
      "Intensity filter at >N signals", """
      Target intensity percentage retained. Above the signal threshold, the signals are filtered to include only the top N signals that make up X% of the intensity. This might include fewer than the specified number of signals.
      """, 0.98, 0.1, 1d);

  public static final IntegerParameter cropToMaxSignals = new IntegerParameter(
      "Crop to top N signals", """
      Crop spectrum to the top N most abundant signals.
      """, 250, 2, null);

  public SignalFiltersParameters() {
    super(removePrecursor, cropToMaxSignals, signalThresholdIntensityFilter,
        intensityPercentFilter);
  }

  public SpectralSignalFilter createFilter() {
    return createFilter(this);
  }

  public static SpectralSignalFilter createFilter(ParameterSet params) {
    boolean isRemovePrecursor = params.getValue(SignalFiltersParameters.removePrecursor);
    var removePrecursorMz = params.getEmbeddedParameterValueIfSelectedOrElse(
        SignalFiltersParameters.removePrecursor, 0d);
    var cropToMaxSignals = params.getValue(SignalFiltersParameters.cropToMaxSignals);
    var signalThresholdForTargetIntensityPercent = params.getValue(
        SignalFiltersParameters.signalThresholdIntensityFilter);
    var targetIntensityPercentage = params.getValue(SignalFiltersParameters.intensityPercentFilter);
    return new SpectralSignalFilter(isRemovePrecursor, removePrecursorMz, cropToMaxSignals,
        signalThresholdForTargetIntensityPercent, targetIntensityPercentage);
  }

  public SignalFiltersParameters setValue(SpectralSignalFilter filter) {
    setParameter(removePrecursor, filter.isRemovePrecursor(), filter.removePrecursorMz());
    setParameter(intensityPercentFilter, filter.targetIntensityPercentage());
    setParameter(signalThresholdIntensityFilter, filter.signalThresholdForTargetIntensityPercent());
    setParameter(cropToMaxSignals, filter.cropToMaxSignals());
    return this;
  }
}
