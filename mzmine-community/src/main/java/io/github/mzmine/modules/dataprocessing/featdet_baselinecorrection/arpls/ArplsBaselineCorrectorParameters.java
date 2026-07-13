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

package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.arpls;

import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.AbstractBaselineCorrectorParameters;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import java.text.DecimalFormat;

public class ArplsBaselineCorrectorParameters extends AbstractBaselineCorrectorParameters {

  public static final double DEFAULT_LAMBDA = 1e5;
  public static final double DEFAULT_RATIO = 1e-6;
  public static final int DEFAULT_MAX_ITERATIONS = 50;

  public static final DoubleParameter lambda = new DoubleParameter("Smoothness (lambda)", """
      Smoothing strength of the baseline.
      Larger values yield a stiffer, smoother baseline that ignores narrow signals; smaller values
      let the baseline follow the data more closely, but may start to follow peaks.
      Typical range 1e3 - 1e7. Default: 1e5.
      """, new DecimalFormat("0.###E0"), DEFAULT_LAMBDA, 1d, 1e12);

  public static final IntegerParameter maxIterations = new IntegerParameter("Max iterations", """
      The maximum number of asymmetric reweighting iterations.
      The fit usually converges well before this limit. Default: 50.
      """, DEFAULT_MAX_ITERATIONS, 1, 500);

  public ArplsBaselineCorrectorParameters() {
    super(applyPeakRemoval.cloneParameter(), lambda, maxIterations);
  }
}
