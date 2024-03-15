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
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Scales a vector to the standard deviation of its values.
 */
public class AutoScalingFunction implements ScalingFunction {

  private final StandardDeviation dev = new StandardDeviation(true);

  @Override
  public @NotNull String getName() {
    return "Auto scaling";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return AutoScalingParameters.class;
  }

  public AutoScalingFunction createInstance(ParameterSet parameters) {
    return new AutoScalingFunction();
  }

  @Override
  public RealVector apply(RealVector input) {
    final double sd = dev.evaluate(input.toArray());
    return input.mapDivide(sd);
  }
}
