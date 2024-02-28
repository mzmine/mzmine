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

package io.github.mzmine.modules.tools.isotopepatternpreview;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.util.ExitCode;

/**
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class IsotopePatternPreviewParameters extends SimpleParameterSet {

  public static final StringParameter formula = new StringParameter("Chemical formula",
      "The element/formula to calculate the isotope pattern of. Enter a sum formula.");

  public static final DoubleParameter mergeWidth = new DoubleParameter("Merge width (m/z)",
      "This will be used to merge isotope compositions in the calculated isotope pattern if they overlap.",
      MZmineCore.getConfiguration().getMZFormat(), 0.0001, 0.0000000001, 10.0d);

  public static final PercentParameter minIntensity = new PercentParameter("Minimum intensity",
      "The minimum natural abundance of an isotope and normalized intensity in the calculated isotope pattern.\n"
          + "Min = 0.0, Max = 0.99...", 0.001, 0.0, 0.9999999999);

  public static final IntegerParameter charge = new IntegerParameter("Charge",
      "Enter a charge to apply to the molecule. (e.g. [M]+ = +1 / [M]- = -1\n"
          + "This can also be set to 0 to plot the exact mass.", 1, true);

  public static final BooleanParameter applyFit = new BooleanParameter("Apply fit (visual)",
      "Shows a fitted curve for each signal.", false);

  public IsotopePatternPreviewParameters() {
    super(new Parameter[]{formula, minIntensity, mergeWidth, charge, applyFit});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    if ((getParameters() == null) || (getParameters().length == 0)) {
      return ExitCode.OK;
    }

    ParameterSetupDialog dialog = new IsotopePatternPreviewDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
