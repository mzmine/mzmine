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

package io.github.mzmine.modules.dataprocessing.filter_interestingfeaturefinder;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;

public class IsomerQualityParameters extends SimpleParameterSet {

  public static final DoubleParameter minIntensity = new DoubleParameter("Minimum intensity",
      "Minimum intensity of a possible isomer to be annotated.",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E3, 0d, 1E10);

  public static final IntegerParameter minDataPointsInTrace = new IntegerParameter(
      "Minimum number of datapoints in trace",
      "Minimum number of data points in ion mobility trace to be recognised as a"
          + " isomeric compound.\nUsed to filter out noise after resolving.",
      30, 1, 500);

  public IsomerQualityParameters() {
    super(new Parameter[]{minIntensity, minDataPointsInTrace});
  }
}
