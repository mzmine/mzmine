/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
