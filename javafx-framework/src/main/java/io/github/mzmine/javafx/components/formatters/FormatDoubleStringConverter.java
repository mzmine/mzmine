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

package io.github.mzmine.javafx.components.formatters;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;

/**
 * Similar to {@link NumberStringConverter}
 */
public class FormatDoubleStringConverter extends StringConverter<Double> {

  private final NumberFormat numberFormat;

  /**
   * Constructs a {@code FormatDoubleStringConverter} with the default locale and format.
   */
  public FormatDoubleStringConverter() {
    this(Locale.getDefault());
  }

  /**
   * Constructs a {@code FormatDoubleStringConverter} with the given locale and the default format.
   *
   * @param locale the locale used in determining the number format used to format the string
   */
  public FormatDoubleStringConverter(Locale locale) {
    this(locale, null);
  }

  /**
   * Constructs a {@code FormatDoubleStringConverter} with the default locale and the given decimal
   * format pattern.
   *
   * @param pattern the string pattern used in determining the number format used to format the
   *                string
   * @see java.text.DecimalFormat
   */
  public FormatDoubleStringConverter(String pattern) {
    this(Locale.getDefault(), pattern);
  }

  /**
   * Constructs a {@code FormatDoubleStringConverter} with the given locale and decimal format
   * pattern.
   *
   * @param locale  the locale used in determining the number format used to format the string
   * @param pattern the string pattern used in determining the number format used to format the
   *                string
   * @see java.text.DecimalFormat
   */
  public FormatDoubleStringConverter(Locale locale, String pattern) {
    this(locale, pattern, null);
  }

  /**
   * Constructs a {@code FormatDoubleStringConverter} with the given number format.
   *
   * @param numberFormat the number format used to format the string
   */
  public FormatDoubleStringConverter(NumberFormat numberFormat) {
    this(null, null, numberFormat);
  }

  FormatDoubleStringConverter(Locale locale, String pattern, NumberFormat numberFormat) {
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
  public Double fromString(String value) {
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
      return numberFormat.parse(value).doubleValue();
    } catch (ParseException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString(Double value) {
    // If the specified value is null, return a zero-length String
    if (value == null) {
      return "";
    }

    // Perform the requested formatting
    return numberFormat.format(value);
  }

}