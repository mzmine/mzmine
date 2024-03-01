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
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.util.ExitCode;

/**
 * @description Rubber Band baseline corrector parameters.
 *
 */
public class RubberBandCorrectorParameters extends SimpleParameterSet {

  /**
   * Noise level.
   */
  public static final DoubleParameter NOISE = new DoubleParameter("noise",
      "Ignored if \"auto noise\" is checked. Noise level to be taken into account.",
      DecimalFormat.getNumberInstance(), 0.0, 0.0, null);

  /**
   * Determine noise automatically.
   */
  public static final BooleanParameter AUTO_NOISE = new BooleanParameter("auto noise",
      "Determine noise level automatically (from lower intensity scan).", false);

  /**
   * Degree of Freedom.
   */
  public static final DoubleParameter DF = new DoubleParameter("df", "Degree of freedom.",
      DecimalFormat.getNumberInstance(), 0.0, 0.0, null);

  /**
   * Interpolating with spline.
   */
  public static final BooleanParameter SPLINE = new BooleanParameter("spline",
      "Logical indicating whether the baseline should be an interpolating spline through the support points or piecewise linear.",
      true);

  /**
   * Bend additional feature.
   */
  public static final DoubleParameter BEND_FACTOR = new DoubleParameter("bend factor",
      "Does nothing if equals to zero. Helps fitting better with low \"df\". Try with 5E4, to start palying with...",
      DecimalFormat.getNumberInstance(), 0.0, 0.0, null);

  public RubberBandCorrectorParameters() {
    super(new UserParameter[] {NOISE, AUTO_NOISE, DF, SPLINE, BEND_FACTOR});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    BaselineCorrectorSetupDialog dialog =
        new BaselineCorrectorSetupDialog(valueCheckRequired, this, RubberBandCorrector.class);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
