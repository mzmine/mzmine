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

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import java.util.function.Function;
import org.apache.commons.math3.linear.RealVector;

/**
 * This interface describes the scaling of the intensities or areas from a feature table prior to
 * statistical analysis. A statistician recommended "AutoScaling" as a default, which scales
 * intensities to the standard deviation of the row.
 */
public interface ScalingFunction extends Function<RealVector, RealVector>, MZmineModule {

  public ScalingFunction createInstance(ParameterSet parameters);
}
