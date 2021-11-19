/*
 * Copyright 2006-2021 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_scanfilters.mean;

import java.awt.Window;

import io.github.mzmine.modules.dataprocessing.filter_scanfilters.ScanFilterSetupDialog;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.util.ExitCode;

public class MeanFilterParameters extends SimpleParameterSet {

  public static final DoubleParameter oneSidedWindowLength =
      new DoubleParameter("Window length", "One-sided length of the smoothing window");

  public MeanFilterParameters() {
    super(new UserParameter[] {oneSidedWindowLength});
  }

  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    ScanFilterSetupDialog dialog =
        new ScanFilterSetupDialog(valueCheckRequired, this, MeanFilter.class);
    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
