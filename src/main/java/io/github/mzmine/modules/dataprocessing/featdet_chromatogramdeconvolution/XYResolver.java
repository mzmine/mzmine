/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.modules.MZmineModule;
import java.util.Collection;
import java.util.List;

/**
 * Interface for an resolver. This resolver should be usable multiple times without
 * reinitialisation. A parameter set can be passed in the constructor of the resolver. If R is
 * required, the resolver should be able to initialise it's own instance in the constructor. See
 * {@link ResolvingUtil} and {@link FeatureResolverTask} for an example use-case.
 *
 * @param <Rx> Return type x value
 * @param <Ry> Return type y value
 * @param <X>  X-value type
 * @param <Y>  Y-value type
 */
public interface XYResolver<Rx, Ry, X, Y> extends MZmineModule {

  /**
   * See implementing classes for more detailed information on possible restrictions on x and y data
   * such as ordering.
   *
   * @param x domain values of the data to be resolved.
   * @param y range values of the data to be resolved.
   * @return Collection of a Set of indices for each resolved peak.
   */
  public Collection<List<ResolvedValue<Rx, Ry>>> resolve(X x, Y y);
}
