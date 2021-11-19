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

package io.github.mzmine.util.maths;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class Precision {

  private static DecimalFormat format = new DecimalFormat("0.0E0");

  /**
   * Checks if difference of a and b is small equal to max difference
   *
   * @param a        value 1
   * @param b        value 2
   * @param maxDelta maximum difference
   * @return true if Math.abs(a-b) <= maxDelta
   */
  public static boolean equals(double a, double b, double maxDelta) {
    return Math.abs(a - b) <= maxDelta;
  }

  /**
   * Checks if difference of a and b is small equal to max difference
   *
   * @param a        value 1
   * @param b        value 2
   * @param maxDelta maximum difference
   * @return true if Math.abs(a-b) <= maxDelta
   */
  public static boolean equals(float a, float b, float maxDelta) {
    return Math.abs(a - b) <= maxDelta;
  }

  /**
   * Checks if difference of a and b is smaller equal to the maximum of the relative and absolute
   * delta
   *
   * @param a                value 1
   * @param b                value 2
   * @param maxDelta         maximum difference
   * @param maxRelativeDelta relative delta (as a factor to the maximum of a and b)
   * @return true if Math.abs(a-b) <= maxDelta
   */
  public static boolean equals(double a, double b, double maxDelta, double maxRelativeDelta) {
    // diff <= max of relative and absolute
    return Math.abs(a - b) <= Math.max(Math.max(a, b) * maxRelativeDelta, maxDelta);
  }

  /**
   * Checks if difference of a and b is smaller equal to the maximum of the relative and absolute
   * delta
   *
   * @param a                value 1
   * @param b                value 2
   * @param maxDelta         maximum difference
   * @param maxRelativeDelta relative delta (as a factor to the maximum of a and b)
   * @return true if Math.abs(a-b) <= maxDelta
   */
  public static boolean equals(float a, float b, float maxDelta, float maxRelativeDelta) {
    // diff <= max of relative and absolute
    return Math.abs(a - b) <= Math.max(Math.max(a, b) * maxRelativeDelta, maxDelta);
  }


  /**
   * Uses RoundingMode.HALF_UP
   *
   * @param value
   * @param sig
   * @return A BigDecimal rounded to sig number of significant digits (figures)
   */
  public static BigDecimal round(double value, int sig) {
    return round(value, sig, RoundingMode.HALF_UP);
  }

  /**
   * @param value
   * @param sig
   * @param mode
   * @return A BigDecimal rounded to sig number of significant digits (figures)
   */
  public static BigDecimal round(double value, int sig, RoundingMode mode) {
    MathContext mc = new MathContext(sig, RoundingMode.HALF_UP);
    BigDecimal bigDecimal = new BigDecimal(value, mc);
    return bigDecimal;
  }

  /**
   * Uses RoundingMode.HALF_UP
   *
   * @param value
   * @param sig
   * @return A String of the value rounded to sig number of significant digits (figures)
   */
  public static String toString(double value, int sig) {
    return round(value, sig).toPlainString();
  }

  /**
   * @param value
   * @param sig
   * @param mode
   * @return A String of the value rounded to sig number of significant digits (figures)
   */
  public static String toString(double value, int sig, RoundingMode mode) {
    return round(value, sig, mode).toPlainString();
  }

  /**
   * Uses RoundingMode.HALF_UP. If the number of digits exceeds maxLength, the format is changed to
   * scientific notation with E as x10.
   *
   * @param value
   * @param sig
   * @return A String of the value rounded to sig number of significant digits (figures)
   */
  public static String toString(double value, int sig, int maxLength) {
    return toString(value, sig, RoundingMode.HALF_UP, maxLength);
  }

  /**
   * If the number of digits exceeds maxLength, the format is changed to scientific notation with E
   * as x10
   *
   * @param value
   * @param sig
   * @param mode
   * @param maxLength maximum length (number of digits)
   * @return A String of the value rounded to sig number of significant digits (figures)
   */
  public static String toString(double value, int sig, RoundingMode mode, int maxLength) {
    BigDecimal dec = round(value, sig, mode);
    String str = dec.toPlainString();
    int digits = str.replaceAll("[^0-9]", "").length();
    if (digits <= maxLength) {
      return str;
    } else {
      format.setMaximumFractionDigits(maxLength - 1);
      return format.format(dec);
    }
  }

}
