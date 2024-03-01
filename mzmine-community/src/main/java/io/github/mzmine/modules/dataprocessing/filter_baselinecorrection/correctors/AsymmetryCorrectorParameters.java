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

package io.github.mzmine.modules.dataprocessing.filter_baselinecorrection.correctors;

import java.text.DecimalFormat;
import io.github.mzmine.modules.dataprocessing.filter_baselinecorrection.BaselineCorrectorSetupDialog;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.util.ExitCode;

/**
 * @description Asymmetric baseline corrector parameters.
 *
 */
public class AsymmetryCorrectorParameters extends SimpleParameterSet {

  /**
   * Smoothing factor.
   */
  public static final DoubleParameter SMOOTHING = new DoubleParameter("Smoothing",
      "The smoothing factor (>= 0), generally 10^5 - 10^8, the larger it is, the smoother the baseline will be.",
      DecimalFormat.getNumberInstance(), null, 0.0, null);

  /**
   * Asymmetry.
   */
  public static final DoubleParameter ASYMMETRY = new DoubleParameter("Asymmetry",
      "The weight (0 <= p <= 1) for points above the trend line, whereas 1-p is the weight for points below it. Naturally, p should be small for estimating baselines.",
      DecimalFormat.getNumberInstance(), 0.001, 0.0, 1.0);

  public AsymmetryCorrectorParameters() {
    super(new UserParameter[] {SMOOTHING, ASYMMETRY});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    BaselineCorrectorSetupDialog dialog =
        new BaselineCorrectorSetupDialog(valueCheckRequired, this, AsymmetryCorrector.class);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
