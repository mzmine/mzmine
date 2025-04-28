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

package io.github.mzmine.util.date;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parse and format dates. MZmine uses {@link LocalDateTime} to represent date + time in this
 * format: 2022-06-01T18:36:09
 * <p>
 * 2022-06-01T18:36:09Z is a zoned format that needs to be parsed by {@link ZonedDateTime}
 * <p>
 * For {@link LocalDate} parsing look at {@link LocalDateTimeParser#parseAnyFirstDate(String)}
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class DateTimeUtils {

  /**
   * Obtains an instance of LocalDateTime from a text string such as 2007-12-03T10:15:30. The string
   * must represent a valid date-time and is parsed using DateTimeFormatter.ISO_LOCAL_DATE_TIME.
   *
   * @param dateTime the text to parse such as "2007-12-03T10:15:30" or "2007-12-03T10:15:30Z"
   * @return the parsed local date-time, not null
   * @throws java.time.format.DateTimeParseException – if the text cannot be parsed
   */
  @NotNull
  public static LocalDateTime parse(@NotNull String dateTime) {
    try {
      // ZonedDateTime with 2022-06-01T18:36:09Z where the Z stands for UTC
      final ZonedDateTime zoned = ZonedDateTime.parse(dateTime);
      return getStandardUtcLocalTime(zoned);
    } catch (Exception ignored) {
      // try to parse LocalDateTime 2022-06-01T18:36:09
      try {
        return LocalDateTime.parse(dateTime);
      } catch (Exception _) {
        return LocalDateTimeParser.parseAnyFirstDate(dateTime);
      }
    }
  }

  /**
   * Actually shifting the time instant internally
   *
   * @param zoned
   * @return
   */
  public static @NotNull ZonedDateTime getStandardUtcTime(ZonedDateTime zoned) {
    return zoned.withZoneSameInstant(ZoneOffset.UTC);
  }

  /**
   * Actually shifting the time instant internally
   *
   * @param zoned
   * @return
   */
  public static @NotNull LocalDateTime getStandardUtcLocalTime(ZonedDateTime zoned) {
    return getStandardUtcTime(zoned).toLocalDateTime();
  }

  /**
   * Obtains an instance of LocalDateTime from a text string such as 2007-12-03T10:15:30. The string
   * must represent a valid date-time and is parsed using DateTimeFormatter.ISO_LOCAL_DATE_TIME.
   *
   * @param dateTime the text to parse such as "2007-12-03T10:15:30" or "2007-12-03T10:15:30Z"
   * @return the parsed local date-time or default value on error or if input was null
   */
  public static LocalDateTime parseOrElse(final String dateTime,
      final @Nullable LocalDateTime defaultValue) {
    if (dateTime == null) {
      return defaultValue;
    }
    try {
      return DateTimeUtils.parse(dateTime);
    } catch (Exception ignored) {
      return defaultValue;
    }
  }

}
