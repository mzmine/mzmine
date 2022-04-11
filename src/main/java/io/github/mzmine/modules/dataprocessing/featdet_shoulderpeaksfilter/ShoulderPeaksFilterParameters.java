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

package io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.util.ExitCode;

public class ShoulderPeaksFilterParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final DoubleParameter resolution = new DoubleParameter("Mass resolution",
      "Mass resolution is the dimensionless ratio of the mass of the peak divided by its width."
          + "\nPeak width is taken as the full width at half maximum intensity (FWHM).");

  public static final ComboParameter<PeakModelType> peakModel =
      new ComboParameter<PeakModelType>("Peak model function",
          "Peaks under the curve of this peak model will be removed", PeakModelType.values());

  public ShoulderPeaksFilterParameters() {
    super(new Parameter[] {dataFiles, resolution, peakModel});

  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    ShoulderPeaksFilterSetupDialog dialog =
        new ShoulderPeaksFilterSetupDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

}
