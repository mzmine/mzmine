/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.visualization.rawdataoverviewims;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

public class IMSRawDataOverviewParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter rawDataFiles = new RawDataFilesParameter(1, 1);

  public static final DoubleParameter summedFrameNoiseLevel = new DoubleParameter("Frame noise"
      + " level", "Noise level for the summed frame spectrum.",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E3);

  public static final DoubleParameter mobilityScanNoiseLevel = new DoubleParameter("Mobility sca "
      + "noise level", "Noise level for individual mobility scans, mobilogram and heatmap "
      + "calculation.", MZmineCore.getConfiguration().getIntensityFormat(), 5E2);

  public IMSRawDataOverviewParameters() {
    super(new UserParameter[]{rawDataFiles, summedFrameNoiseLevel, mobilityScanNoiseLevel});
  }
}
