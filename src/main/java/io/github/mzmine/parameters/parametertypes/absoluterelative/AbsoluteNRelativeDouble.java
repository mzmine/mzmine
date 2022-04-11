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
public class AbsoluteNRelativeDouble {

  private final double abs;
  private final double rel;

  public AbsoluteNRelativeDouble(final double abs, final double rel) {
    this.abs = abs;
    this.rel = rel;
  }

  public double getAbsolute() {
    return abs;
  }

  public double getRelative() {
    return rel;
  }

  /**
   * Maximum of absolute or value*relative (e.g., to define a threshold)
   * 
   * @param value
   * @return
   */
  public double getMaximumValue(double value) {
    return Math.max(abs, value * rel);
  }

  /**
   * Minimum of absolute or value*relative (e.g., to define a threshold)
   * 
   * @param value
   * @return
   */
  public double getMinimumValue(double value) {
    return Math.min(abs, value * rel);
  }

  /**
   * 
   * @param total is the total number to calculate with the relative
   * @param value value to check
   * @return
   */
  public boolean checkGreaterEqualMax(double total, double value) {
    return value >= getMaximumValue(total);
  }

  public boolean checkGreaterMax(double total, double value) {
    return value > getMaximumValue(total);
  }

  public boolean checkLessEqualMax(double total, double value) {
    return value <= getMaximumValue(total);
  }

  public boolean checkLessMax(double total, double value) {
    return value < getMaximumValue(total);
  }

  public boolean checkEqualMax(double total, double value) {
    return value == getMaximumValue(total);
  }


  public Range<Double> getRange(final double value) {
    return Range.closed(getMinimumValue(value), getMaximumValue(value));
  }

  @Override
  public String toString() {
    return MessageFormat.format("abs={0} and rel={1}", abs, rel);
  }

  public boolean isGreaterZero() {
    return rel > 0 || abs > 0;
  }
}
