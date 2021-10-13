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

package io.github.mzmine.modules.dataprocessing.featdet_smoothing.loess;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;

public class LoessSmoothingParameters extends SimpleParameterSet {

  public static final OptionalParameter<IntegerParameter> rtSmoothing = new OptionalParameter<>(
      new IntegerParameter("Retention time width (scans)",
          "Enables intensity smoothing along the rt axis.", 5, 0, Integer.MAX_VALUE));

  public static final OptionalParameter<IntegerParameter> mobilitySmoothing = new OptionalParameter<IntegerParameter>(
      new IntegerParameter("Mobility width (scans)",
          "Enables intensity smoothing along the mobility axis.", 5, 0, Integer.MAX_VALUE));

  public LoessSmoothingParameters() {
    super(new Parameter[]{rtSmoothing, mobilitySmoothing});
  }
}
