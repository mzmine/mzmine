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
