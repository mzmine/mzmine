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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.factor_of_lowest;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectorSetupDialog;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.util.ExitCode;

public class FactorOfLowestMassDetectorParameters extends SimpleParameterSet {

  public static final DoubleParameter noiseFactor = new DoubleParameter("Noise factor",
      "Signals less than lowest intensity x noiseFactor are removed",
      MZmineCore.getConfiguration().getScoreFormat(), 2.5);

  public FactorOfLowestMassDetectorParameters() {
    super(new UserParameter[]{noiseFactor},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_mass_detection/mass-detection-algorithms.html#factor-of-the-lowest-signal");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    MassDetectorSetupDialog dialog = new MassDetectorSetupDialog(valueCheckRequired,
        FactorOfLowestMassDetector.class, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

}
