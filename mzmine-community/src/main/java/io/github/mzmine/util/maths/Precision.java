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
import org.jetbrains.annotations.Nullable;

public class Precision {

  private static final DecimalFormat exponentFormat = new DecimalFormat("0.0E0");

  /**
   * Checks if difference of a and b is small equal to max difference
   *
   * @param a        value 1
   * @param b        value 2
   * @param maxDelta maximum difference
   * @return true if Math.abs(a-b) <= maxDelta
   */
  public static boolean equals(double a, double b, double maxDelta) {
    if (Math.abs(a - b) <= maxDelta) {
      return true;
    }
    // both are NaN or both are - or + INFINITY
    return Double.compare(a, b) == 0;
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
    final float delta = Math.abs(a - b);
    if (delta <= maxDelta) {
      return true;
    }
    // both are NaN or both are - or + INFINITY
    return Float.compare(a, b) == 0;
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
    // equal or both are NaN
    if (Double.compare(a, b) == 0) {
      return true;
    }
    // only one is NaN
    if (Double.isNaN(a) || Double.isNaN(b)) {
      return false;
    }
    // abs error
    final double diff = Math.abs(a - b);
    if (diff <= maxDelta) {
      return true;
    }

    final double absoluteMax = Math.max(Math.abs(a), Math.abs(b));
    final double relativeDifference = Math.abs(diff / absoluteMax);
    return relativeDifference <= maxRelativeDelta;
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
    // equal or both are NaN
    if (Float.compare(a, b) == 0) {
      return true;
    }
    // only one is NaN
    if (Float.isNaN(a) || Float.isNaN(b)) {
      return false;
    }
    // abs error
    final double diff = Math.abs(a - b);
    if (diff <= maxDelta) {
      return true;
    }

    final double absoluteMax = Math.max(Math.abs(a), Math.abs(b));
    final double relativeDifference = Math.abs(diff / absoluteMax);
    return relativeDifference <= maxRelativeDelta;
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
    MathContext mc = new MathContext(sig, mode);
    BigDecimal bigDecimal = BigDecimal.valueOf(value).round(mc);
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
      // format as 1E5 notation
      return exponentFormat.format(dec);
    }
  }

  public static boolean equalDoubleSignificance(final @Nullable Number a,
      final @Nullable Number b) {
    if (a == null && b == null) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }
    return equalDoubleSignificance(a.doubleValue(), b.doubleValue());
  }

  public static boolean equalFloatSignificance(final @Nullable Number a, final @Nullable Number b) {
    if (a == null && b == null) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }
    return equalFloatSignificance(a.doubleValue(), b.doubleValue());
  }

  /**
   * Uses relative error of 5E-5
   *
   * @param a first value
   * @param b second value
   * @return true if the relative difference diff/max is <= relative error. true if both values are
   * NaN, false if only one is NaN
   */
  public static boolean equalFloatSignificance(final double a, final double b) {
    return equalRelativeSignificance(a, b, 5E-6);
  }

  /**
   * Uses relative error of 5E-13
   *
   * @param a first value
   * @param b second value
   * @return true if the relative difference diff/max is <= relative error. true if both values are
   * NaN, false if only one is NaN
   */
  public static boolean equalDoubleSignificance(final double a, final double b) {
    return equalRelativeSignificance(a, b, 5E-13);
  }


  /**
   *
   * @param a             first value
   * @param b             second value
   * @param relativeError relative error that is accepted, e.g., 1E-2, 0.01 of 1%
   * @return true if the relative difference diff/max is <= relative error. true if both values are
   * NaN, false if only one is NaN
   */
  public static boolean equalRelativeSignificance(final double a, final double b,
      final double relativeError) {
    // equal or both are NaN
    if (Double.compare(a, b) == 0) {
      return true;
    }
    // only one is NaN
    if (Double.isNaN(a) || Double.isNaN(b)) {
      return false;
    }

    final double absoluteMax = Math.max(Math.abs(a), Math.abs(b));
    final double relativeDifference = Math.abs((a - b) / absoluteMax);

    // relative difference may be infinite for overflow, but this is handled correctly
    return relativeDifference <= relativeError;
  }
}
