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

package io.github.mzmine.javafx.components.formatters;

import com.google.common.collect.Range;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import org.jetbrains.annotations.NotNull;

/**
 * Similar to {@link NumberStringConverter}
 */
public class FormatDoubleRangeStringConverter extends StringConverter<Range<Double>> {

  private final NumberFormat numberFormat;

  /**
   * Constructs a {@code FormatDoubleStringConverter} with the default locale and format.
   */
  public FormatDoubleRangeStringConverter() {
    this(Locale.getDefault());
  }

  /**
   * Constructs a {@code FormatDoubleStringConverter} with the given locale and the default format.
   *
   * @param locale the locale used in determining the number format used to format the string
   */
  public FormatDoubleRangeStringConverter(Locale locale) {
    this(locale, null);
  }

  /**
   * Constructs a {@code FormatDoubleStringConverter} with the default locale and the given decimal
   * format pattern.
   *
   * @param pattern the string pattern used in determining the number format used to format the
   *                string
   * @see DecimalFormat
   */
  public FormatDoubleRangeStringConverter(String pattern) {
    this(Locale.getDefault(), pattern);
  }

  /**
   * Constructs a {@code FormatDoubleStringConverter} with the given locale and decimal format
   * pattern.
   *
   * @param locale  the locale used in determining the number format used to format the string
   * @param pattern the string pattern used in determining the number format used to format the
   *                string
   * @see DecimalFormat
   */
  public FormatDoubleRangeStringConverter(Locale locale, String pattern) {
    this(locale, pattern, null);
  }

  /**
   * Constructs a {@code FormatDoubleStringConverter} with the given number format.
   *
   * @param numberFormat the number format used to format the string
   */
  public FormatDoubleRangeStringConverter(NumberFormat numberFormat) {
    this(null, null, numberFormat);
  }

  FormatDoubleRangeStringConverter(Locale locale, String pattern, NumberFormat numberFormat) {
    if (locale == null) {
      locale = Locale.getDefault();
    }

    if (numberFormat == null) {
      if (pattern != null) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(locale);
        numberFormat = new DecimalFormat(pattern, symbols);
      } else {
        numberFormat = NumberFormat.getNumberInstance(locale);
      }
    }
    assert numberFormat != null;
    this.numberFormat = numberFormat;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Range<Double> fromString(String value) {
    try {
      // If the specified value is null or zero-length, return null
      if (value == null) {
        return null;
      }

      value = value.trim();

      if (value.isEmpty()) {
        return null;
      }

      // Perform the requested parsing
      return getSingleValueToCeilDecimalRangeOrRange(value);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString(Range<Double> value) {
    // If the specified value is null, return a zero-length String
    if (value == null) {
      return "";
    }

    // Perform the requested formatting
    return numberFormat.format(value);
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
   * Parses string of the given filter text field and returns a range of values satisfying the
   * filter. Examples: "5.34" -> [5.34 - epsilon, 5.34 + epsilon] "2.37 - 6" -> [2.37 - epsilon,
   * 6.00 + epsilon]
   *
   * @param filterStr filter string, either a single value or a range as 1-2
   * @return Range of values satisfying the filter or RangeUtils.DOUBLE_NAN_RANGE if the filter
   * string is invalid
   */
  @NotNull
  public static Range<Double> getSingleValueToCeilDecimalRangeOrRange(@NotNull String filterStr) {
    // need to remove spaces around the range definition
    filterStr = filterStr.trim().replace(" ", "");

    if (filterStr.isEmpty()) { // Empty filter
      return Range.all();
    } else if (filterStr.contains("-") && filterStr.indexOf("-") > 0) { // Filter by range
      try {
        return parseDoubleRange(filterStr);
      } catch (Exception exception) {
        return Range.singleton(Double.NaN);
      }
    } else { // Filter by single value
      try {
        return getRangeToCeilDecimal(filterStr);
      } catch (Exception exception) {
        return Range.singleton(Double.NaN);
      }
    }
  }
}