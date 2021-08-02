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
import javax.annotation.concurrent.Immutable;

import com.google.common.collect.Range;

/**
 * This class represents rt tolerance. Tolerance is set using either a absolute (sec) or relative
 * (%) value.
 */
@Immutable
public class RTTolerance {

  // Tolerance can be either absolute (sec) or relative (%).
  private final @Nonnull Float rtTolerance;
  private final boolean isAbsolute;

  /**
   * <p>
   * Constructor for RTTolerance.
   * </p>
   *
   * @param rtTolerance a {@link Float} object.
   * @param isAbsolute a {@link Boolean} object.
   */
  public RTTolerance(final float rtTolerance, final boolean isAbsolute) {
    this.rtTolerance = rtTolerance;
    this.isAbsolute = isAbsolute;
  }

  /**
   * <p>
   * isAbsolute.
   * </p>
   *
   * @return a boolean.
   */
  public boolean isAbsolute() {
    return isAbsolute;
  }

  /**
   * <p>
   * getTolerance.
   * </p>
   *
   * @return a float.
   */
  public float getTolerance() {
    return rtTolerance;
  }

  /**
   * <p>
   * getToleranceRange.
   * </p>
   *
   * @param rtValue a float.
   * @return a {@link Range} object.
   */
  public Range<Float> getToleranceRange(final float rtValue) {
    final float absoluteTolerance = isAbsolute ? rtTolerance : rtValue * rtTolerance;
    return Range.closed(rtValue - absoluteTolerance, rtValue + absoluteTolerance);
  }

  /**
   * <p>
   * checkWithinTolerance.
   * </p>
   *
   * @param rt1 a float.
   * @param rt2 a float.
   * @return a boolean.
   */
  public boolean checkWithinTolerance(final float rt1, final float rt2) {
    return getToleranceRange(rt1).contains(rt2);
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return isAbsolute ? rtTolerance + " sec" : 100.0 * rtTolerance + " %";
  }

}
