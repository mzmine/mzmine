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

package io.github.mzmine.modules.dataprocessing.filter_scanfilters.roundresample;

import java.awt.Window;

import io.github.mzmine.modules.dataprocessing.filter_scanfilters.ScanFilterSetupDialog;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.util.ExitCode;

public class RndResampleFilterParameters extends SimpleParameterSet {

  public static final BooleanParameter SUM_DUPLICATES = new BooleanParameter(
      "Sum duplicate intensities",
      "Concatenates/sums ions count (intensity) of m/z peaks competing for being rounded at same m/z unit. "
          + "If unchecked, the intensities are averaged rather than summed.",
      false);

  public static final BooleanParameter REMOVE_ZERO_INTENSITY =
      new BooleanParameter("Remove zero intensity m/z peaks",
          "Clear all scans spectra from m/z peaks with intensity equal to zero.", false);

  public RndResampleFilterParameters() {
    super(new Parameter[] {SUM_DUPLICATES, REMOVE_ZERO_INTENSITY});
  }

  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    ScanFilterSetupDialog dialog =
        new ScanFilterSetupDialog(valueCheckRequired, this, RndResampleFilter.class);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
