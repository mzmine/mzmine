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

package io.github.mzmine.util.deconvolution;

import java.util.Collection;

/**
 *
 * @param <R> Return type, e.g. Set<Y data type>
 * @param <X> X-value type
 * @param <Y> Y-value type
 */
public interface XYResolver<R, X, Y> {

  /**
   * See implementing classes for more detailed information on possible restrictions on x and y data
   * such as ordering.
   *
   * @param x domain values of the data to be resolved.
   * @param y range values of the data to be resolved.
   * @return Collection of a Set of indices for each resolved peak.
   */
  public Collection<R> resolveToYData(X x, Y y);
}
