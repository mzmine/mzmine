/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

/*
 * Code created was by or on behalf of Syngenta and is released under the open source license in use
 * for the pre-existing code or project. Syngenta does not assert ownership or copyright any over
 * pre-existing work.
 */

package io.github.mzmine.util;

import com.google.common.collect.Range;
import io.github.mzmine.util.maths.ArithmeticUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RangeUtils {

  /**
   * [-inf, +inf] range.
   */
  public static final Range<Double> DOUBLE_INFINITE_RANGE
      = Range.closed(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

  /**
   * [NaN, NaN] range.
   */
  public static final Range<Double> DOUBLE_NAN_RANGE
      = Range.singleton(Double.NaN);

  /**
   * Parses a range from String where upper and lower bounds are delimited by a dash, e.g.
   * "100.0-200.5". Note: we are dealing with doubles, so in an unfortunate case the range might
   * look like this "3.402439E-36-1.310424E-2".
   */
  public static Range<Double> parseDoubleRange(String text) {
    Pattern p = Pattern.compile("([\\d\\.]+(?:E\\-?\\d+)?)\\-([\\d\\.]+(?:E\\-?\\d+)?)");

    Matcher m = p.matcher(text);
    if (!m.find()) {
      throw new IllegalArgumentException("String '" + text + "' could not be parsed into a range");
    }
    double low = Double.parseDouble(m.group(1));
    double high = Double.parseDouble(m.group(2));
    return Range.closed(low, high);
  }

  /**
   * @param number        A double value
   * @param decimalPlaces the number of decimal places to round the range to.
   * @return A range starting at the given number with the given precision up to the next decimal.
   * E.g.: 5.3 -> [5.3 -> 5.4), 5.324 -> [5.324 -> 5.325); 5 -> [5, 6)
   */
  public static Range<Double> getRangeToCeilDecimal(String str) throws NumberFormatException {
    String filterStr = str.trim();
    final int decimalIndex = filterStr.indexOf(".");
    final int decimalPlaces = decimalIndex != -1 ? filterStr.length() - decimalIndex - 1 : 0;
    final double parsed = Double.parseDouble(filterStr);
    final double num = parsed + Math.pow(10, (decimalPlaces + 1) * -1);
    final double lower = new BigDecimal(num).setScale(decimalPlaces, RoundingMode.DOWN)
        .doubleValue();
    final double upper = new BigDecimal(num).setScale(decimalPlaces, RoundingMode.UP)
        .doubleValue();
    return Range.closedOpen(lower, upper);
  }

  /**
   * Converts given range to Float range.
   *
   * @param range Input range
   * @return Converted Float range
   */
  public static <N extends Number & Comparable<N>> Range<Float> toFloatRange(Range<N> range) {
    return Range.closed(range.lowerEndpoint().floatValue(), range.upperEndpoint().floatValue());
  }

  /**
   * Converts given range to Double range.
   *
   * @param range Input range
   * @return Converted Double range
   */
  public static <N extends Number & Comparable<N>> Range<Double> toDoubleRange(Range<N> range) {
    return Range.closed(range.lowerEndpoint().doubleValue(), range.upperEndpoint().doubleValue());
  }


  /**
   * Splits the range in numOfBins bins and then returns the index of the bin which contains given
   * value. Indexes are from 0 to (numOfBins - 1).
   *
   * @param range     Input range
   * @param numOfBins Number of bins
   * @param value     Value inside the range
   * @return Index of the bin containing given value
   */
  public static <N extends Number & Comparable<N>> int binNumber(Range<N> range, int numOfBins,
      N value) {
    N rangeLength = rangeLength(range);
    N valueDistanceFromStart = ArithmeticUtils.subtract(value, range.lowerEndpoint());
    return (int) Math.round(ArithmeticUtils.multiply(ArithmeticUtils
        .divide(valueDistanceFromStart, rangeLength), (numOfBins - 1)).doubleValue());
  }

  /**
   * Returns length of the given range. i.e. [a..b] -> b - a
   *
   * @param range Range
   * @return Range length
   */
  public static <N extends Number & Comparable<N>> N rangeLength(Range<N> range) {
    return ArithmeticUtils.subtract(range.upperEndpoint(), range.lowerEndpoint());
  }

  /**
   * Returns central value of the given range. i.e. [a..b] -> [a + b] / 2
   *
   * @param range Range
   * @return Range center
   */
  public static <N extends Number & Comparable<N>> N rangeCenter(Range<N> range) {
    return ArithmeticUtils.divide(
        ArithmeticUtils.add(range.upperEndpoint(), range.lowerEndpoint()), (N) (Number) 2.0f);
  }

  /**
   * Constructs a range from the given array.
   *
   * @param array Input array
   * @return Output range
   */
  public static <N extends Number & Comparable<N>> Range<N> fromArray(N[] array) {
    if ((array == null) || (array.length == 0)) {
      return Range.open((N) (Number) 0.0f, (N) (Number) 0.0f);
    }

    N min = array[0], max = array[0];
    for (N d : array) {
      if (d.compareTo(max) > 0) {
        max = d;
      }
      if (d.compareTo(min) < 0) {
        min = d;
      }
    }
    return Range.closed(min, max);
  }

  /**
   * Returns a range that is contained in between the both ranges.
   *
   * @param r1 First range
   * @param r2 Second range
   * @return The connected range. Null if there is no connected range.
   */
  public static @Nullable
  <N extends Number & Comparable<N>> Range<N> getConnected(@Nonnull Range<N> r1,
      @Nonnull Range<N> r2) {

    if (!r1.isConnected(r2)) {
      return null;
    }

    N lower = (r1.lowerEndpoint().compareTo(r2.lowerEndpoint()) > 0)
        ? r1.lowerEndpoint()
        : r2.lowerEndpoint();
    N upper = (r1.upperEndpoint().compareTo(r2.upperEndpoint()) > 0)
        ? r2.upperEndpoint()
        : r1.upperEndpoint();

    return Range.closed(lower, upper);
  }

  /**
   * Checks if the given range equals to [NaN, NaN].
   *
   * @param range Range
   * @return True if the range equals to [NaN, NaN], false otherwise
   */
  public static <N extends Number & Comparable<N>> boolean isNaNRange(@Nonnull Range<N> range) {
    return Double.isNaN(range.lowerEndpoint().doubleValue())
        && Double.isNaN(range.upperEndpoint().doubleValue());
  }

  public static boolean isJFreeRangeConnectedToGoogleRange(org.jfree.data.Range jfreeRange,
      Range<? extends Number> googleRange) {
    return jfreeRange.contains(googleRange.lowerEndpoint().doubleValue()) || jfreeRange
        .contains(googleRange.upperEndpoint().doubleValue());
  }
}
