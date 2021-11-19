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

package io.github.mzmine.parameters.parametertypes.tolerances;

import com.google.common.collect.Range;

/**
 * RTTolerance allows specifying retention time tolerance it is either absolute (seconds or minutes)
 * or relative (percent) but as rest of MZmine codebase, it assumes that rt values (other than the
 * tolerance given in constructor) are in minutes in methods such as getToleranceRange or
 * checkWithinTolerance
 */
public class RTTolerance {

  private final float tolerance;
  private final Unit unit;

  public RTTolerance(final float rtTolerance, Unit unit) {
    this.tolerance = rtTolerance;
    this.unit = unit;
  }

  // old constructor for compatibility with other mzmine branches and pull requests
  public RTTolerance(final boolean isAbsolute, final float rtTolerance) {
    this(rtTolerance, isAbsolute ? Unit.MINUTES : Unit.PERCENT);
  }

  public boolean isAbsolute() {
    return unit.isAbsolute();
//    return unit == Unit.SECONDS || unit == Unit.MINUTES;
  }

  public float getTolerance() {
    return tolerance;
  }

  public Unit getUnit() {
    return unit;
  }

  public Range<Float> getToleranceRange(final float rtValue) {
    // rtValue is given in minutes
    float absoluteTolerance;
    switch (unit) {
      case SECONDS:
        absoluteTolerance = tolerance / 60;
        break;
      case PERCENT:
        absoluteTolerance = rtValue * (tolerance / 100);
        break;
      case MINUTES:
      default:
        absoluteTolerance = tolerance;
        break;
    }
    return Range.closed(rtValue - absoluteTolerance, rtValue + absoluteTolerance);
  }

  public float getToleranceInMinutes() {
    return switch (unit) {
      case SECONDS -> tolerance / 60;
      case PERCENT -> 5f * 60f * (tolerance
          / 100); // having getToleranceMethod is inconsistent with the unit percent being there...
      case MINUTES -> tolerance;
    };
  }

  public boolean checkWithinTolerance(final float rt1, final float rt2) {

    return getToleranceRange(rt1).contains(rt2);
  }

  @Override
  public String toString() {
    return tolerance + " " + unit.toString();
  }

  public enum Unit {
    MINUTES, SECONDS, PERCENT;

    public boolean isAbsolute() {
      return this == SECONDS || this == MINUTES;
    }

    @Override
    public String toString() {
      switch (this) {
        case SECONDS:
          return "seconds";
        case MINUTES:
          return "minutes";
        case PERCENT:
          return "%";
        default:
          throw new IllegalArgumentException();
      }
    }
  }

}
