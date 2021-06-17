/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.parameters.parametertypes.absoluterelative;

import java.text.MessageFormat;
import com.google.common.collect.Range;

/**
 * This parameter holds a relative and absolute value/threshold/tolerance
 */
public class AbsoluteNRelativeInt {

  public enum Mode {
    ROUND, ROUND_UP, ROUND_DOWN;
  }

  private Mode mode;
  private final int abs;
  private final float rel;

  public AbsoluteNRelativeInt(final int abs, final float rel) {
    this.abs = abs;
    this.rel = rel;
  }

  public AbsoluteNRelativeInt(final int abs, final float rel, Mode mode) {
    this.abs = abs;
    this.rel = rel;
    this.mode = mode;
  }

  public int getAbsolute() {
    return abs;
  }

  public float getRelative() {
    return rel;
  }

  /**
   * Maximum of absolute or value*relative (e.g., to define a threshold)
   * 
   * @param value
   * @return
   */
  public int getMaximumValue(int value) {
    float v2 = value * rel;
    if (abs >= v2)
      return abs;
    else
      switch (mode) {
        case ROUND:
          return Math.round(v2);
        case ROUND_DOWN:
          return (int) Math.floor(v2);
        case ROUND_UP:
          return (int) Math.ceil(v2);
      }
    return 0;
  }

  /**
   * Minimum of absolute or value*relative (e.g., to define a threshold)
   * 
   * @param value
   * @return
   */
  public int getMinimumValue(int value) {
    float v2 = value * rel;
    if (abs <= v2)
      return abs;
    else
      switch (mode) {
        case ROUND:
          return Math.round(v2);
        case ROUND_DOWN:
          return (int) Math.floor(v2);
        case ROUND_UP:
          return (int) Math.ceil(v2);
      }
    return 0;
  }

  /**
   * 
   * @param total is the total number to calculate with the relative
   * @param value value to check
   * @return
   */
  public boolean checkGreaterEqualMax(int total, double value) {
    return value >= getMaximumValue(total);
  }

  public boolean checkGreaterMax(int total, double value) {
    return value > getMaximumValue(total);
  }

  public boolean checkLessEqualMax(int total, double value) {
    return value <= getMaximumValue(total);
  }

  public boolean checkLessMax(int total, double value) {
    return value < getMaximumValue(total);
  }

  public boolean checkEqualMax(int total, double value) {
    return value == getMaximumValue(total);
  }


  public Range<Integer> getRange(final int total) {
    return Range.closed(getMinimumValue(total), getMaximumValue(total));
  }

  @Override
  public String toString() {
    return MessageFormat.format("abs={0} and rel={1}", abs, rel);
  }

  public boolean isGreaterZero() {
    return rel > 0 || abs > 0;
  }
}
