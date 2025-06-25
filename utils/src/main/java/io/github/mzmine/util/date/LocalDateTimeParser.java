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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/**
 * Currently this only supports date formats valid in file paths. US format with / slash is not a
 * compatible character for paths.
 * <p>
 * Result is always a LocalDateTime just for ease of use. If no time is defined the time will be set
 * to the start of the day 00:00:00.
 * <p>
 * ZonedDataTime will be converted to UTC times. The format {@link #ZONED_MODIFIED_ISO_DATE_TIME} is
 * adapted for file paths: 2025-12-24T05:50:55_CET or * 2025-12-24T05:50:55_+0230 for +2:30
 */
public enum LocalDateTimeParser {
  // uses (?!\d) as look ahead to make date followed by another number illegal.
  // date and time
  /**
   * ZonedDateTime - will be converted to LocalDateTime:
   * <p>
   * yyyy-MM-dd_HH-MM-SS or yyyy-MM-ddTHH-MM-SS followed by a time zone abbreviation like CET or UTC
   * or by _+HHmm for an hour and minute offset. So the full format is 2025-12-24T05:50:55_CET or
   * 2025-12-24T05:50:55_+0230 for +2:30
   */
  ZONED_MODIFIED_ISO_DATE_TIME(
      new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd") // Standard date format
          .optionalStart().appendLiteral('T').optionalEnd() // Optional 'T' separator
          .optionalStart().appendLiteral('_').optionalEnd() // Optional 'T' separator
          .appendPattern("HH-mm-ss") // Time format
          .optionalStart().appendLiteral('_')
          .appendZoneOrOffsetId()  // Optional time zone abbreviation (e.g., CET, UTC)
          .optionalEnd().optionalStart().appendLiteral('_')
          .appendOffset("+HHmm", "Z")  // Optional offset format (e.g., +0200 or Z for UTC)
          .optionalEnd().toFormatter(Locale.ENGLISH),
      // regex to match with optional parts
      "\\d{4}-\\d{2}-\\d{2}" +                  // Matches YYYY-MM-DD
          "(?:[T_]\\d{2}-\\d{2}-\\d{2})" +             // Optional 'T' or _ separator + HH-MM-SS
          "(?:_(?:[A-Z]{2,4}|[+-]\\d{2}\\d{2}|Z))" // Optional TimeZone abbreviation or Offset
      , true, true),
  /**
   * yyyy-MM-dd_HH-MM-SS or yyyy-MM-ddTHH-MM-SS
   */
  MODIFIED_ISO_DATE_TIME(
      new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd") // Standard date format
          .optionalStart().appendLiteral('T').optionalEnd() // Optional 'T' separator
          .optionalStart().appendLiteral('_').optionalEnd() // Optional 'T' separator
          .appendPattern("HH-mm-ss") // Time format
          .optionalStart().appendLiteral('_')
          .appendZoneOrOffsetId()  // Optional time zone abbreviation (e.g., CET, UTC)
          .optionalEnd().optionalStart().appendLiteral('_')
          .appendOffset("+HHmm", "Z")  // Optional offset format (e.g., +02:00 or Z for UTC)
          .optionalEnd().toFormatter(Locale.ENGLISH),
      // regex to match with optional parts
      "\\d{4}-\\d{2}-\\d{2}" +                  // Matches YYYY-MM-DD
          "(?:[T_]\\d{2}-\\d{2}-\\d{2})"            // Optional 'T' or _ separator + HH-MM-SS
      , true, false),

  // just dates
  BASIC_ISO_DATE(DateTimeFormatter.BASIC_ISO_DATE, "\\d{8}"), // 20241231 yyyyMMdd
  ISO_DATE(DateTimeFormatter.ISO_DATE, "\\d{4}-\\d{2}-\\d{2}"), // yyyy-MM-dd
  EUROPEAN_DATE("dd.MM.yyyy", "\\d{2}\\.\\d{2}\\.\\d{4}"), //
  JAPANESE_DATE("yyyy.MM.dd", "\\d{4}\\.\\d{2}\\.\\d{2}"); //

  private static final Logger logger = Logger.getLogger(LocalDateTimeParser.class.getName());

  private final DateTimeFormatter formatter;
  private final Pattern pattern;
  private final Pattern patternStarts;
  private final Pattern patternEnds;
  private final boolean timed;
  private final boolean zoned;

  LocalDateTimeParser(String pattern, String regex) {
    this(DateTimeFormatter.ofPattern(pattern), regex);
  }

  LocalDateTimeParser(final DateTimeFormatter formatter, final String regex) {
    this(formatter, regex, false, false);
  }

  LocalDateTimeParser(DateTimeFormatter formatter, String regex, boolean timed, boolean zoned) {
    this.formatter = formatter;
    this.pattern = Pattern.compile(regex + "(?!\\d)"); // disallow trailing numbers
    this.patternStarts = Pattern.compile("^" + regex + "(?!\\d)"); // disallow trailing numbers
    this.patternEnds = Pattern.compile(regex + "$");
    this.timed = timed;
    this.zoned = zoned;
  }

  public DateTimeFormatter getFormatter() {
    return formatter;
  }

  public Pattern getPattern() {
    return pattern;
  }

  /**
   * @param input any string that contains a date pattern anywhere in the string
   * @return the local date
   * @throws DateTimeParseException
   */
  public @Nullable LocalDateTime parseFirst(String input) {
    final Matcher matcher = pattern.matcher(input);
    if (matcher.find()) {
      if (zoned) {
        return DateTimeUtils.getStandardUtcLocalTime(
            ZonedDateTime.parse(matcher.group(0), formatter));
      } else if (timed) {
        return LocalDateTime.parse(matcher.group(0), formatter);
      } else {
        return LocalDate.parse(matcher.group(0), formatter).atStartOfDay();
      }
    }
    return null;
  }

  /**
   * @param input any string that starts with a date pattern
   * @return the local date
   * @throws DateTimeParseException
   */
  public @Nullable LocalDateTime parseStart(String input) {
    final Matcher matcher = patternStarts.matcher(input);
    if (matcher.find()) {
      if (zoned) {
        return DateTimeUtils.getStandardUtcLocalTime(
            ZonedDateTime.parse(matcher.group(0), formatter));
      } else if (timed) {
        return LocalDateTime.parse(matcher.group(0), formatter);
      } else {
        return LocalDate.parse(matcher.group(0), formatter).atStartOfDay();
      }
    }
    return null;
  }

  /**
   * @param input any string that ends with a date pattern
   * @return the local date
   * @throws DateTimeParseException
   */
  public @Nullable LocalDateTime parseEnd(String input) {
    final Matcher matcher = patternEnds.matcher(input);
    if (matcher.find()) {
      if (zoned) {
        return DateTimeUtils.getStandardUtcLocalTime(
            ZonedDateTime.parse(matcher.group(0), formatter));
      } else if (timed) {
        return LocalDateTime.parse(matcher.group(0), formatter);
      } else {
        return LocalDate.parse(matcher.group(0), formatter).atStartOfDay();
      }
    }
    return null;
  }

  /**
   * @param input any string that contains a date pattern anywhere in the string
   * @return the local date
   */
  public static @Nullable LocalDateTime parseAnyFirstDate(String input) {
    for (final LocalDateTimeParser parser : values()) {
      try {
        final LocalDateTime date = parser.parseFirst(input);
        if (date != null) {
          return date;
        }
      } catch (DateTimeParseException ex) {
        // silent and try next
        logger.log(Level.FINE, ex.getMessage(), ex);
      }
    }
    return null;
  }

  /**
   * @param input any string that starts with a date pattern
   * @return the local date
   */
  public static @Nullable LocalDateTime parseAnyStartingDate(String input) {
    for (final LocalDateTimeParser parser : values()) {
      try {
        final LocalDateTime date = parser.parseStart(input);
        if (date != null) {
          return date;
        }
      } catch (DateTimeParseException ex) {
        // silent and try next
      }
    }
    return null;
  }

  /**
   * @param input any string that ends with a date pattern
   * @return the local date
   */
  public static @Nullable LocalDateTime parseAnyEndingDate(String input) {
    for (final LocalDateTimeParser parser : values()) {
      try {
        final LocalDateTime date = parser.parseEnd(input);
        if (date != null) {
          return date;
        }
      } catch (DateTimeParseException ex) {
        // silent and try next
      }
    }
    return null;
  }

}