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
 * ConstantPpmTolerance class.
 * </p>
 */
public class ConstantPpmTolerance implements MzTolerance {

  // PPM conversion factor.
  private static final Double MILLION = 1_000_000.0;
  private final @Nonnull Double ppmTolerance;

  /**
   * <p>
   * Constructor for ConstantPpmTolerance.
   * </p>
   *
   * @param tolerancePPM a {@link Double} object.
   */
  public ConstantPpmTolerance(final @Nonnull Double tolerancePPM) {
    ppmTolerance = tolerancePPM;
  }

  /**
   * <p>
   * Getter for the field <code>ppmTolerance</code>.
   * </p>
   *
   * @return a {@link Double} object.
   */
  public @Nonnull Double getPpmTolerance() {
    return ppmTolerance;
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * Return the mass tolerance range that is constant in ppm, but changes vs. m/z.
   * </p>
   */
  public @Nonnull Range<Double> getToleranceRange(final @Nonnull Double mzValue) {
    final @Nonnull Double absoluteTolerance = mzValue / MILLION * ppmTolerance;
    return Range.closed(mzValue - absoluteTolerance, mzValue + absoluteTolerance);
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return ppmTolerance + " ppm";
  }

}
