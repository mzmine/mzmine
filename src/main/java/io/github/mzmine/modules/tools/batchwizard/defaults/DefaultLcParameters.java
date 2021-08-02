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

package io.github.mzmine.modules.tools.batchwizard.defaults;

import io.github.mzmine.modules.tools.batchwizard.BatchWizardHPLCParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;

public class DefaultLcParameters {

  public static final DefaultLcParameters uhplc = new DefaultLcParameters(
      new RTTolerance(0.05f, Unit.MINUTES), new RTTolerance(0.05f, Unit.MINUTES),
      new RTTolerance(0.1f, Unit.MINUTES));

  public static final DefaultLcParameters hplc = new DefaultLcParameters(
      new RTTolerance(0.1f, Unit.MINUTES), new RTTolerance(0.05f, Unit.MINUTES),
      new RTTolerance(0.1f, Unit.MINUTES));

  private final RTTolerance fwhm;
  private final RTTolerance intraSampleTolerance;
  private final RTTolerance interSampleTolerance;

  public DefaultLcParameters(RTTolerance fwhm, RTTolerance intraSampleTolerance,
      RTTolerance interSampleTolerance) {
    this.fwhm = fwhm;
    this.intraSampleTolerance = intraSampleTolerance;
    this.interSampleTolerance = interSampleTolerance;
  }

  public void setToParameterSet(ParameterSet parameterSet) {
    parameterSet.getParameter(BatchWizardHPLCParameters.approximateChromatographicFWHM)
        .setValue(fwhm);
    parameterSet.getParameter(BatchWizardHPLCParameters.intraSampleRTTolerance)
        .setValue(intraSampleTolerance);
    parameterSet.getParameter(BatchWizardHPLCParameters.interSampleRTTolerance)
        .setValue(interSampleTolerance);
  }
}
