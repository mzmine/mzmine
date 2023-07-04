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

package io.github.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra;

import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SignalFiltersParameters;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.scans.similarity.Weights;

public class MirrorScanParameters extends SimpleParameterSet {

  public static final MZToleranceParameter mzTol = new MZToleranceParameter("m/z tolerance",
      "Tolerance to match signals in both scans", 0.0025, 20);

  public static final ComboParameter<Weights> weight = new ComboParameter<>("Weights",
      "Weights for m/z and intensity", Weights.VALUES, Weights.SQRT);

  public static final ParameterSetParameter<SignalFiltersParameters> signalFilters = new ParameterSetParameter<>(
      "Signal filters", """
      Signal filters to limit the number of signals etc.
      """, new SignalFiltersParameters());

  /**
   * Windows size and position
   */
  public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

  public MirrorScanParameters() {
    super(mzTol, weight, signalFilters, windowSettings);
  }

  @Override
  public int getVersion() {
    // changed to signal filters for version 2
    return 2;
  }
}
