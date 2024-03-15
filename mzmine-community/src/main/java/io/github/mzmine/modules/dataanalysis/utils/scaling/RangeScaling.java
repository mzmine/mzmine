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

import io.github.mzmine.parameters.ParameterSet;
import org.apache.commons.math3.linear.RealVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RangeScaling implements ScalingFunction {

  private final double maxValue;

  public RangeScaling() {
    this(0);
  }

  public RangeScaling(double maxValue) {
    this.maxValue = maxValue;
  }

  @Override
  public @NotNull String getName() {
    return "Range scaling";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return RangeScalingParameters.class;
  }

  @Override
  public ScalingFunction createInstance(ParameterSet parameters) {
    return new RangeScaling(parameters.getValue(RangeScalingParameters.maximumValue));
  }

  @Override
  public RealVector apply(RealVector realVector) {
    final double columnMax = realVector.getLInfNorm();
    return realVector.mapDivide(columnMax / maxValue);
  }
}
