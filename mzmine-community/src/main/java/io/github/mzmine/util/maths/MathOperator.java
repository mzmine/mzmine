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

package io.github.mzmine.util.maths;

/**
 * A list of math operators and their string representation
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public enum MathOperator {
  GREATER(">"), GREATER_EQ("≥"), EQUAL("="), NOT_EQUAL("≠"), LESS("<"), LESS_EQ("≤");

  private final String s;

  MathOperator(String s) {
    this.s = s;
  }

  @Override
  public String toString() {
    return s;
  }

  public boolean checkValue(double base, double testedVal) {
    return switch (this) {
      case LESS -> testedVal < base;
      case LESS_EQ -> testedVal <= base;
      case GREATER -> testedVal > base;
      case GREATER_EQ -> testedVal >= base;
      case EQUAL -> testedVal == base;
      case NOT_EQUAL -> testedVal != base;
    };
  }
}
