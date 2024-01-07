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
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.util.ExitCode;

/**
 * @description Feature Detection baseline corrector parameters.
 *
 */
public class PeakDetectionCorrectorParameters extends SimpleParameterSet {

  /**
   * Smallest peak width.
   */
  public static final IntegerParameter LEFT = new IntegerParameter("left (number of scans)",
      "Smallest window size for peak widths (in number of scans).", 1, 0, null);
  /**
   * Largest peak width.
   */
  public static final IntegerParameter RIGHT = new IntegerParameter("right (number of scans)",
      "Largest window size for peak widths (in number of scans).", 1, 0, null);

  /**
   * Smallest minimums and medians spectra removal.
   */
  public static final IntegerParameter LWIN = new IntegerParameter("lwin (number of scans)",
      "Smallest window size for minimums and medians in peak removed spectra (in number of scans).",
      1, 0, null);
  /**
   * Largest minimums and medians spectra removal.
   */
  public static final IntegerParameter RWIN = new IntegerParameter("rwin (number of scans)",
      "Largest window size for minimums and medians in peak removed spectra (in number of scans).",
      1, 0, null);

  /**
   * Minimum signal to noise ratio.
   */
  public static final DoubleParameter SNMINIMUM =
      new DoubleParameter("snminimum", "Minimum signal to noise ratio for accepting peaks.",
          DecimalFormat.getNumberInstance(), 0.0, 0.0, 1.0);

  /**
   * Monotonically decreasing baseline.
   */
  public static final DoubleParameter MONO =
      new DoubleParameter("mono", "Monotonically decreasing baseline if ‘mono’>0.",
          DecimalFormat.getNumberInstance(), 0.0, 0.0, null);

  /**
   * Window size multiplier.
   */
  public static final DoubleParameter MULTIPLIER = new DoubleParameter("multiplier",
      "Internal window size multiplier.", DecimalFormat.getNumberInstance(), 1.0, 1.0, null);

  public PeakDetectionCorrectorParameters() {
    super(new UserParameter[] {LEFT, RIGHT, LWIN, RWIN, SNMINIMUM, MONO, MULTIPLIER});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    BaselineCorrectorSetupDialog dialog =
        new BaselineCorrectorSetupDialog(valueCheckRequired, this, PeakDetectionCorrector.class);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
