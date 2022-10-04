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

package io.github.mzmine.datamodel;

/**
 * Represents the polarity of ionization.
 */
public enum PolarityType {

  POSITIVE(+1, "+"), //
  NEGATIVE(-1, "-"), //
  NEUTRAL(0, "n"), //
  UNKNOWN(0, "?");

  private final int sign;
  private final String charValue;

  PolarityType(int sign, String charValue) {
    this.sign = sign;
    this.charValue = charValue;
  }

  /**
   * @return +1 for positive polarity, -1 for negative polarity, and 0 for neutral or unknown
   *         polarity.
   */
  public int getSign() {
    return sign;
  }

  public String asSingleChar() {
    return charValue;
  }

  public static PolarityType fromSingleChar(String s) {
    for (PolarityType p : values()) {
      if (p.charValue.equals(s))
        return p;
    }
    return UNKNOWN;
  }

  public static PolarityType fromInt(int i) {
    if(i == 0) {
      return UNKNOWN;
    } else if(i < 0) {
      return NEGATIVE;
    } else {
      return POSITIVE;
    }
  }
}
