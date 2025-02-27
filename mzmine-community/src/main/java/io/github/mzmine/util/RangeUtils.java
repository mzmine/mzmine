/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.util;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import io.github.mzmine.util.maths.ArithmeticUtils;
import io.github.mzmine.util.maths.Precision;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RangeUtils {

  /**
   * [-inf, +inf] range.
   */
  public static final Range<Double> DOUBLE_INFINITE_RANGE = Range.closed(Double.NEGATIVE_INFINITY,
      Double.POSITIVE_INFINITY);

  /**
   * [NaN, NaN] range.
   */
  public static final Range<Double> DOUBLE_NAN_RANGE = Range.singleton(Double.NaN);

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

  public static Range<Double> getRangeToCeilDecimal(String str) throws NumberFormatException {
    String filterStr = str.trim();
    final int decimalIndex = filterStr.indexOf(".");
    final int decimalPlaces = decimalIndex != -1 ? filterStr.length() - decimalIndex - 1 : 0;
    final double parsed = Double.parseDouble(filterStr);
    final double num = parsed + Math.pow(10, (decimalPlaces + 1) * -1);
    final double lower = new BigDecimal(num).setScale(decimalPlaces, RoundingMode.DOWN)
        .doubleValue();
    final double upper = new BigDecimal(num).setScale(decimalPlaces, RoundingMode.UP).doubleValue();
    return Range.closedOpen(lower, upper);
  }

  /**
   * Converts given range to Float range.
   *
   * @param range Input range
   * @return Converted Float range
   */
  public static <N extends Number & Comparable<N>> Range<Float> toFloatRange(Range<N> range) {
    if (!(range.hasLowerBound() && range.hasUpperBound())) {
      return Range.all();
    }
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
    return (int) Math.round(
        ArithmeticUtils.multiply(ArithmeticUtils.divide(valueDistanceFromStart, rangeLength),
            (numOfBins - 1)).doubleValue());
  }

  /**
   * Returns length of the given range. i.e. [a..b] -> b - a
   *
   * @param range Range
   * @return Range length
   */
  public static <N extends Number & Comparable<?>> N rangeLength(Range<N> range) {
    return ArithmeticUtils.subtract(range.upperEndpoint(), range.lowerEndpoint());
  }

  /**
   * Returns central value of the given range. i.e. [a..b] -> [a + b] / 2
   *
   * @param range Range
   * @return Range center
   */
  public static <N extends Number & Comparable<N>> N rangeCenter(Range<N> range) {
    return ArithmeticUtils.divide(ArithmeticUtils.add(range.upperEndpoint(), range.lowerEndpoint()),
        (N) (Number) 2.0f);
  }

  /**
   * The score of how close testedValue is to range center
   *
   * @param testedValue defines the output type
   * @param range       center of and length of range are used
   * @param <N>         any number type
   * @return score from 0 - 1, 1 perfectly centered and 0 fully on the edge. 1 -
   * abs(center-testedValue)/(range.length/2).
   */
  public static <N extends Number & Comparable<N>> float calcCenterScore(final float testedValue,
      final @NotNull Range<N> range) {
    return Math.max(0, 1 - calcRelativeDeltaFromCenterAbs(testedValue, range));
  }

  /**
   * The score of how close testedValue is to range center
   *
   * @param testedValue defines the output type
   * @param range       center of and length of range are used
   * @param <N>         any number type
   * @return score from 0 - 1, 1 perfectly centered and 0 fully on the edge. 1 -
   * abs(center-testedValue)/(range.length/2).
   */
  public static <N extends Number & Comparable<N>> double calcCenterScore(final double testedValue,
      final @NotNull Range<N> range) {
    return Math.max(0, 1 - calcRelativeDeltaFromCenterAbs(testedValue, range));
  }

  /**
   * The relative difference of testedValue to center of range
   *
   * @param testedValue defines the output type
   * @param range       center of and length of range are used
   * @param <N>         any number type
   * @return score from 0 - 1, 0 perfectly centered and 1 fully on the edge.
   * abs(center-testedValue)/ (range.length/2). May be higher than 1 if range.contains(testedValue)
   * == false.
   */
  public static <N extends Number & Comparable<N>> float calcRelativeDeltaFromCenterAbs(
      final float testedValue, final @NotNull Range<N> range) {
    float lower = range.lowerEndpoint().floatValue();
    float upper = range.upperEndpoint().floatValue();
    float halfLength = (upper - lower) / 2f;
    float center = lower + halfLength;
    return Math.abs(center - testedValue) / halfLength;
  }

  /**
   * The relative difference of testedValue to center of range
   *
   * @param testedValue defines the output type
   * @param range       center of and length of range are used
   * @param <N>         any number type
   * @return score from 0 - 1, 0 perfectly centered and 1 fully on the edge.
   * abs(center-testedValue)/ (range.length/2). May be higher than 1 if range.contains(testedValue)
   * == false.
   */
  public static <N extends Number & Comparable<N>> double calcRelativeDeltaFromCenterAbs(
      final double testedValue, final @NotNull Range<N> range) {
    double lower = range.lowerEndpoint().doubleValue();
    double upper = range.upperEndpoint().doubleValue();
    double halfLength = (upper - lower) / 2d;
    double center = lower + halfLength;
    return Math.abs(center - testedValue) / halfLength;
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
  public static @Nullable <N extends Number & Comparable<N>> Range<N> getConnected(
      @NotNull Range<N> r1, @NotNull Range<N> r2) {

    if (!r1.isConnected(r2)) {
      return null;
    }

    N lower = (r1.lowerEndpoint().compareTo(r2.lowerEndpoint()) > 0) ? r1.lowerEndpoint()
        : r2.lowerEndpoint();
    N upper = (r1.upperEndpoint().compareTo(r2.upperEndpoint()) > 0) ? r2.upperEndpoint()
        : r1.upperEndpoint();

    return Range.closed(lower, upper);
  }

  /**
   * Checks if the given range equals to [NaN, NaN].
   *
   * @param range Range
   * @return True if the range equals to [NaN, NaN], false otherwise
   */
  public static <N extends Number & Comparable<N>> boolean isNaNRange(@NotNull Range<N> range) {
    return Double.isNaN(range.lowerEndpoint().doubleValue()) && Double.isNaN(
        range.upperEndpoint().doubleValue());
  }

  public static boolean isJFreeRangeConnectedToGuavaRange(org.jfree.data.Range jfreeRange,
      Range<? extends Number> guavaRange) {
    if (jfreeRange == null || guavaRange == null) {
      return false;
    }
    return Range.closed(jfreeRange.getLowerBound(), jfreeRange.getUpperBound()).isConnected(
        Range.closed(guavaRange.lowerEndpoint().doubleValue(),
            guavaRange.upperEndpoint().doubleValue()));
  }

  public static boolean isDefaultJFreeRange(org.jfree.data.Range jfreeRange) {
    if (Precision.equals(0d, jfreeRange.getLowerBound(), 0.0001d) && Precision.equals(1d,
        jfreeRange.getUpperBound(), 0.1d)) {
      return true;
    }
    return false;
  }

  public static boolean isJFreeRangeEnclosingGuavaRange(org.jfree.data.Range jfreeRange,
      Range<? extends Number> guavaRange) {
    if (jfreeRange == null || guavaRange == null) {
      return false;
    }
    return jfreeRange.contains(guavaRange.lowerEndpoint().doubleValue()) && jfreeRange.contains(
        guavaRange.upperEndpoint().doubleValue());
  }

  public static boolean isGuavaRangeEnclosingJFreeRange(org.jfree.data.Range jfreeRange,
      Range<? extends Number> guavaRange) {
    if (jfreeRange == null || guavaRange == null) {
      return false;
    }
    Range<Double> r = Range.range(guavaRange.lowerEndpoint().doubleValue(),
        guavaRange.lowerBoundType(), guavaRange.upperEndpoint().doubleValue(),
        guavaRange.upperBoundType());
    return r.contains(jfreeRange.getLowerBound()) && r.contains(jfreeRange.getUpperBound());
  }

  public static org.jfree.data.Range guavaToJFree(Range<? extends Number> range) {
    return new org.jfree.data.Range(range.lowerEndpoint().doubleValue(),
        range.upperEndpoint().doubleValue());
  }

  /**
   * Creates a range that does not have equal start and end points. Does not check for closed or
   * open bounds, compares the boundaries via {@link Comparable#compareTo(Object)}.
   *
   * @param range     The range.
   * @param minLength The min length of the range.
   * @return A range that does not have equal start and end points (can be the original range).
   */
  public static <T extends Number & Comparable> Range<T> getPositiveRange(Range<T> range,
      T minLength) {
    if (range.lowerEndpoint().compareTo(range.upperEndpoint()) == 0) {
      if (range.lowerEndpoint() instanceof Double) {
        return (Range<T>) Range.closed((Double) range.lowerEndpoint(),
            ((Double) range.upperEndpoint()) + (Double) minLength);
      }
      if (range.lowerEndpoint() instanceof Float) {
        return (Range<T>) Range.closed((Float) range.lowerEndpoint(),
            ((Float) range.upperEndpoint()) + (Float) minLength);
      }
      if (range.lowerEndpoint() instanceof Integer) {
        return (Range<T>) Range.closed((Integer) range.lowerEndpoint(),
            ((Integer) range.upperEndpoint()) + (Integer) minLength);
      }
      throw new IllegalArgumentException("The method has not been implemented for this type yet.");
    }
    return range;
  }

  /**
   * This style is used mostly in mzmine
   *
   * @return lower - upper
   */
  public static String formatRange(Range<? extends Number> range, NumberFormat format) {
    return formatRange(range, format, false, false);
  }

  public static String formatRange(Range<? extends Number> range, NumberFormat format,
      boolean guavaStyle, boolean addBounds) {
    String connector = guavaStyle ? ".." : " - ";
    String values =
        format.format(range.lowerEndpoint()) + connector + format.format(range.upperEndpoint());
    if (!addBounds) {
      return values;
    }
    // this style is used for exact range string
    return getBoundString(range, true) + values + getBoundString(range, false);
  }

  @NotNull
  private static String getBoundString(final Range<? extends Number> range, final boolean isLower) {
    if (isLower) {
      return range.lowerBoundType() == BoundType.CLOSED ? "[" : "(";
    }
    return range.upperBoundType() == BoundType.CLOSED ? "]" : ")";
  }

  /**
   * @param center the center of the range
   * @param length the total length of the range, divided by 2 on each side to creat the range.
   */
  public static Range<Double> rangeAround(double center, double length) {
    return Range.closed(center - length / 2, center + length / 2);
  }

  public static Range<Float> rangeAround(float center, float length) {
    return Range.closed(center - length / 2, center + length / 2);
  }

  /**
   * @return true if range is null or unbounded like Range.all() or range.hasLowerBound
   */
  public static boolean isNullOrUnbounded(final @Nullable Range<? extends Number> range) {
    return range == null || !range.hasLowerBound() || !range.hasUpperBound();
  }

  /**
   * @return true if range is bounded. So not null or unbounded like Range.all() or
   * range.hasLowerBound
   */
  public static boolean isBounded(final @Nullable Range<? extends Number> range) {
    return !isNullOrUnbounded(range);
  }

  /**
   * return The wider range (determined by {@link RangeUtils#rangeLength(Range)}.
   */
  public static <T extends Number & Comparable<?>> Range<T> max(Range<T> a, Range<T> b) {
    return rangeLength(a).doubleValue() > rangeLength(b).doubleValue() ? a : b;
  }

  /**
   * @return The smaller range (determined by {@link RangeUtils#rangeLength(Range)}.
   */
  public static <T extends Number & Comparable<?>> Range<T> min(Range<T> a, Range<T> b) {
    return rangeLength(a).doubleValue() < rangeLength(b).doubleValue() ? a : b;
  }
}
