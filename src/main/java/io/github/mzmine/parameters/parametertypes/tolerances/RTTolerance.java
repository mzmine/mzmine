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

package io.github.mzmine.parameters.parametertypes.tolerances;

import com.google.common.collect.Range;

public class RTTolerance {

  // Tolerance can be either absolute (in min) or relative (in %).
  private final boolean isAbsolute;
  private final float tolerance;

  public RTTolerance(final boolean absolute, final float rtTolerance) {

    isAbsolute = absolute;
    tolerance = rtTolerance;
  }

  public boolean isAbsolute() {

    return isAbsolute;
  }

  public double getTolerance() {

    return tolerance;
  }

  public Range<Float> getToleranceRange(final float rtValue) {

    final float absoluteTolerance = isAbsolute ? tolerance : rtValue * tolerance;
    return Range.closed(rtValue - absoluteTolerance, rtValue + absoluteTolerance);
  }

  public boolean checkWithinTolerance(final float rt1, final float rt2) {

    return getToleranceRange(rt1).contains(rt2);
  }

  @Override
  public String toString() {

    return isAbsolute ? tolerance + " min" : 100.0 * tolerance + " %";
  }
}
