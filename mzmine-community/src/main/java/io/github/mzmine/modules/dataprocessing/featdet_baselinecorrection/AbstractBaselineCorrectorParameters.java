/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;

public class AbstractBaselineCorrectorParameters extends SimpleParameterSet {

  public static final BooleanParameter applyPeakRemoval = new BooleanParameter("Exclude peaks",
      "Attempts to remove peaks prior to baseline correction.");

  public static final IntegerParameter numSamples = new IntegerParameter(
      "Number of baseline samples", """
      The number of samples taken from the chromatogram to fit the baseline.
      Too low values may fail to approximate the baseline correctly, too high values may put 
      too much weight on chromatographic signals and distort the baseline. Default: 50.
      """, 50, 2, Integer.MAX_VALUE);

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
}
