/*
 * (C) Copyright 2015-2017 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */

package io.github.msdk.util.tolerances;

import javax.annotation.Nonnull;

import com.google.common.collect.Range;

/**
 * <p>
 * MzTolerance interface.
 * </p>
 */
public interface MzTolerance {

  /**
   * <p>
   * Get a m/z tolerance range for a given m/z.
   * </p>
   *
   * @param mzValue a {@link Double} object.
   * @return a {@link Range} object.
   */
  public @Nonnull Range<Double> getToleranceRange(final @Nonnull Double mzValue);
}
