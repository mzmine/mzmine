/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.util.maths;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class Precision {

  private static DecimalFormat format = new DecimalFormat("0.0E0");

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
   * 
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
   * 
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
   * @param mode
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
    if (digits <= maxLength)
      return str;
    else {
      format.setMaximumFractionDigits(maxLength - 1);
      return format.format(dec);
    }
  }


}
