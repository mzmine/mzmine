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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import java.util.Map;

public class AbstractBaselineCorrectorParameters extends SimpleParameterSet {

  public static final ComboParameter<PeakRemoval> applyPeakRemoval = new ComboParameter<>(
      "Peak detection", "Attempts to remove peaks prior to baseline correction.",
      PeakRemoval.values(), PeakRemoval.WAVELET);

  private final BooleanParameter oldPeakRemoval = new BooleanParameter("Exclude peaks",
      "Attempts to remove peaks prior to baseline correction.");

  public static final PercentParameter samplePercentage = new PercentParameter(
      "Percentage of baseline samples", """
      The approximate number of samples taken from the chromatogram to fit the baseline.
      The actual number might be slightly different for each chromatogram, depending if peak removal is activated.
      Too low values may fail to approximate the baseline correctly, too high values may put
      too much weight on chromatographic signals and distort the baseline. Default: 5%.
      """, 0.05);

  public AbstractBaselineCorrectorParameters() {
    super();
  }

  public AbstractBaselineCorrectorParameters(Parameter<?>... parameters) {
    this(parameters, null);
  }

  public AbstractBaselineCorrectorParameters(String onlineHelpUrl, Parameter<?>... parameters) {
    super(onlineHelpUrl, parameters);
  }

  public AbstractBaselineCorrectorParameters(Parameter<?>[] parameters, String onlineHelpUrl) {
    super(onlineHelpUrl, parameters);
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    var map = super.getNameParameterMap();
    map.put(oldPeakRemoval.getName(), oldPeakRemoval);
    return map;
  }

  @Override
  public void handleLoadedParameters(Map<String, Parameter<?>> loadedParams, int loadedVersion) {
    super.handleLoadedParameters(loadedParams, loadedVersion);

    if (loadedParams.get(oldPeakRemoval.getName()) != null) {
      if (loadedParams.get(oldPeakRemoval) instanceof BooleanParameter p) {
        if (p.getValue() == true) {
          setParameter(applyPeakRemoval, PeakRemoval.LOCAL_MIN);
        } else {
          setParameter(applyPeakRemoval, PeakRemoval.NONE);
        }
      }
    }
  }
}
