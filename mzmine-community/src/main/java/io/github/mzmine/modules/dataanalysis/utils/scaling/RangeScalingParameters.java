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
 *
 */

package io.github.mzmine.modules.dataanalysis.utils.scaling;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import java.text.DecimalFormat;

public class RangeScalingParameters extends SimpleParameterSet {

  public static final DoubleParameter maximumValue = new DoubleParameter("Maximum scaled value",
      "The maximum positive value after scaling was applied. If the data was mean centered before,\n"
          + "the absolute range will be the twice this value (-this -> +this).",
      new DecimalFormat("0.###"), 1d, 0d, Double.MAX_VALUE);

  public RangeScalingParameters() {
    super(maximumValue);
  }
}
