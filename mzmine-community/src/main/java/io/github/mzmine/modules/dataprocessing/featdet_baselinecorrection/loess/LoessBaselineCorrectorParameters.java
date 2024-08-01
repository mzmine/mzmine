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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.loess;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import java.util.Collection;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;

public class LoessBaselineCorrectorParameters extends SimpleParameterSet {

  public static final IntegerParameter numSamples = new IntegerParameter(
      "Number of baseline samples", """
      The number of samples taken from the chromatogram to fit the baseline.
      Higher values may increase the processing time but also lead to better results.
      """, 50, 2, Integer.MAX_VALUE);

  public static final PercentParameter bandwidth = new PercentParameter("Bandwidth",
      "The bandwidth of the LOESS filter.", LoessInterpolator.DEFAULT_BANDWIDTH, 0d, 1d);

  public static final IntegerParameter iterations = new IntegerParameter("Iterations",
      "The number of iterations for the LOESS filter.", LoessInterpolator.DEFAULT_ROBUSTNESS_ITERS,
      1, 10);

  public LoessBaselineCorrectorParameters() {
    super(numSamples, bandwidth, iterations);
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages,
      boolean skipRawDataAndFeatureListParameters) {
    final boolean superCheck = super.checkParameterValues(errorMessages,
        skipRawDataAndFeatureListParameters);

    if (!superCheck) {
      return false;
    }

    if (getValue(bandwidth) * getValue(numSamples) < 2) {
      errorMessages.add("Bandwidth * (number of baseline samples) < 2.");
      return false;
    }

    return true;
  }
}
