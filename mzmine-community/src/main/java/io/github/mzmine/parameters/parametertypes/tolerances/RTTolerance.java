/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
  @Deprecated
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
    float absoluteTolerance = getToleranceInMinutes(rtValue);
    return Range.closed(rtValue - absoluteTolerance, rtValue + absoluteTolerance);
  }

  public float getToleranceInMinutes(float rtValue) {
    return switch (unit) {
      case MINUTES -> tolerance;
      case SECONDS -> tolerance / 60;
      case PERCENT -> rtValue * (tolerance / 100);
    };
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
    // rtValue is given in minutes
    return Math.abs(rt1 - rt2) <= getToleranceInMinutes(rt1);
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
      return switch (this) {
        case SECONDS -> "seconds";
        case MINUTES -> "minutes";
        case PERCENT -> "%";
      };
    }
  }

}
