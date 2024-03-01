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
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.util.ExitCode;

/**
 * @description Local Minima + LOESS baseline corrector parameters.
 *
 */
public class LocMinLoessCorrectorParameters extends SimpleParameterSet {

  /**
   * Method.
   */
  public static final String[] choices = new String[] {"loess", "approx"};
  public static final ComboParameter<String> METHOD = new ComboParameter<String>("method",
      "\"loess\" (smoothed low-percentile intensity) or \"approx\" (linear interpolation).",
      choices, choices[0]);

  /**
   * Determine noise automatically.
   */
  public static final DoubleParameter BW = new DoubleParameter("bw",
      "The bandwidth to be passed to loess.", DecimalFormat.getNumberInstance(), 0.0, 0.0, null);

  /**
   * Number of breaks.
   */
  public static final IntegerParameter BREAKS = new IntegerParameter("breaks",
      "Number of breaks set to M/Z values for finding the local minima or points below a centain quantile of intensities; breaks -1 equally spaced intervals on the log M/Z scale.",
      null, true, 1, null);
  /**
   * Break widthy.
   */
  public static final IntegerParameter BREAK_WIDTH = new IntegerParameter(
      "break width (number of scans)",
      "Overrides \"breaks\" value. Width of a single break. Usually the maximum width (in number of scans) of the largest peak.",
      -1, true, -1, null);
  // TODO: Turn it into Retention Time value rather than number of scans

  /**
   * Quantile feature.
   */
  public static final DoubleParameter QNTL = new DoubleParameter("qntl",
      "If 0, find local minima; if >0 find intensities < qntl*100th quantile locally.",
      DecimalFormat.getNumberInstance(), 0.0d, 0.0d, 1.0d);

  public LocMinLoessCorrectorParameters() {
    super(new UserParameter[] {METHOD, BW, BREAKS, BREAK_WIDTH, QNTL});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    BaselineCorrectorSetupDialog dialog =
        new BaselineCorrectorSetupDialog(valueCheckRequired, this, LocMinLoessCorrector.class);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
