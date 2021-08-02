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

package io.github.mzmine.modules.dataprocessing.featdet_imsexpander;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class ImsExpanderParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final OptionalParameter<MZToleranceParameter> mzTolerance = new OptionalParameter<>(
      new MZToleranceParameter("m/z tolerance",
          "m/z tolerance for peaks in the mobility dimension. If enabled, the given "
              + "tolerance will be applied to the feature m/z. If disabled, the m/z range of the "
              + "feature's data points will be used as a tolerance range."));

  public static final IntegerParameter mobilogramBinWidth = new IntegerParameter(
      "Mobility bin witdh (scans)",
      "The mobility binning width in scans. (default = 1, high mobility resolutions "
          + "in TIMS might require a higher bin width to achieve a constant ion current for a "
          + "mobilogram.", 1, true);

  public ImsExpanderParameters() {
    super(new Parameter[]{featureLists, mzTolerance, mobilogramBinWidth});
  }
}
