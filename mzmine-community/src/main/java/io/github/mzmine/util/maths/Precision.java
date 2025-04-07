/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

  public static boolean equalDoubleSignificance(final double a, final double b) {
    return equalSignificance(a, b, 14); // double significance is 15 - 17 digits
  }

  public static boolean equalSignificance(final double a, final double b, final int sigDigits) {
    if (a == b) {
      return true;
    }
    return round(a, sigDigits).equals(round(b, sigDigits));
    // below is an alternative but this may overflow the double/float so maybe a bad idea
//    double diff = Math.abs(a - b);
//    double larger = Math.max(Math.abs(a), Math.abs(b));
//    return diff <= larger / sigDigits;
  }


  public static boolean equalFloatSignificance(final float a, final float b) {
    return equalSignificance(a, b, 6); // float significance is 6 - 7 digits
  }

  public static boolean equalSignificance(final float a, final float b, final int sigDigits) {
    if (a == b) {
      return true;
    }
    return round(a, sigDigits).equals(round(b, sigDigits));
  }
}
