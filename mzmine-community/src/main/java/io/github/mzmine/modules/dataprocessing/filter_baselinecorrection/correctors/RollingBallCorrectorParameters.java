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

import io.github.mzmine.modules.dataprocessing.filter_baselinecorrection.BaselineCorrectorSetupDialog;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.util.ExitCode;

/**
 * @description Rolling Ball baseline corrector parameters.
 *
 */
public class RollingBallCorrectorParameters extends SimpleParameterSet {

  /**
   * Local minima search window.
   */
  public static final IntegerParameter MIN_MAX_WIDTH = new IntegerParameter("wm (number of scans)",
      "Width of local window for minimization/maximization (in number of scans).", null, 0, null);

  /**
   * Smoothing.
   */
  public static final IntegerParameter SMOOTHING = new IntegerParameter("ws (number of scans)",
      "Width of local window for smoothing (in number of scans).", null, 0, null);

  public RollingBallCorrectorParameters() {
    super(new UserParameter[] {MIN_MAX_WIDTH, SMOOTHING});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    BaselineCorrectorSetupDialog dialog =
        new BaselineCorrectorSetupDialog(valueCheckRequired, this, RollingBallCorrector.class);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
