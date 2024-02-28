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

package io.github.mzmine.parameters.parametertypes.absoluterelative;

import com.google.common.collect.Range;
import java.text.MessageFormat;

/**
 * This parameter holds a relative and absolute value/threshold/tolerance
 */
public record AbsoluteAndRelativeDouble(double abs, double rel) {

  /**
   * Maximum of absolute or value*relative (e.g., to define a threshold)
   *
   * @param value
   * @return
   */
  public double getMaximumValue(double value) {
    double v2 = value * rel;
    if (abs >= v2) {
      return abs;
    } else {
      return v2;
    }
  }

  public double getAbsolute() {
    return abs;
  }

  public double getRelative() {
    return rel;
  }

  /**
   * Minimum of absolute or value*relative (e.g., to define a threshold)
   *
   * @param value
   * @return
   */
  public double getMinimumValue(double value) {
    double v2 = value * rel;
    if (abs <= v2) {
      return abs;
    } else {
      return v2;
    }
  }

  /**
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

  public Range<Double> getRange(final double total) {
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
