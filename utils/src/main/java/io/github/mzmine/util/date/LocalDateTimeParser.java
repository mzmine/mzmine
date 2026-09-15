/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Internal list of supported date and date-time formats. Package private on purpose: all parsing
 * goes through {@link DateTimeUtils}, which is the single entry point for date handling.
 * <p>
 * Supported are the standard ISO formats with colons in the time part (2025-12-24T05:50:55 and
 * 2025-12-24T05:50:55+02:00) as well as path compatible variants that replace the colons by dashes
 * (2025-12-24T05-50-55 and 2025-12-24T05-50-55_CET), because colons are illegal in file paths.
 * <p>
 * Result is always a LocalDateTime just for ease of use. If no time is defined the time will be set
 * to the start of the day 00:00:00. Zoned input is always converted to UTC.
 */
enum LocalDateTimeParser {
  // uses (?!\d) as look ahead to make date followed by another number illegal.
  // order matters: the first format that matches and parses wins. Therefore date-time formats are
  // defined before date only formats and zoned formats before their local counterpart.

  /**
   * Standard ISO date time with time zone or offset, converted to UTC:
   * <p>
   * 2025-12-24T05:50:55Z, 2025-12-24T05:50:55+02:00, 2025-12-24T05:50:55.123+0200, 2025-12-24
   * 05:50:55 CET, or 2025-12-24T05:50:55+02:00[Europe/Berlin]. Date and time may be separated by T,
   * _, or a space.
   */
  ZONED_ISO_DATE_TIME("2025-12-24T05:50:55+02:00", zonedIsoDateTimeFormatter(),
      isoDateTimeRegex() + zoneSuffixRegex(), true, true),
  /**
   * Standard local ISO date time, this is the format mzmine uses internally:
   * <p>
   * 2025-12-24T05:50:55, 2025-12-24T05:50:55.123, or 2025-12-24T05:50. Date and time may be
   * separated by T, _, or a space.
   */
  ISO_DATE_TIME("2025-12-24T05:50:55", isoDateTimeBuilder().toFormatter(Locale.ENGLISH),
      isoDateTimeRegex(), true, false),

  /**
   * ZonedDateTime - will be converted to LocalDateTime:
   * <p>
   * yyyy-MM-dd_HH-MM-SS or yyyy-MM-ddTHH-MM-SS followed by a time zone abbreviation like CET or UTC
   * or by _+HHmm for an hour and minute offset. So the full format is 2025-12-24T05:50:55_CET or
   * 2025-12-24T05:50:55_+0230 for +2:30
   */
  ZONED_MODIFIED_ISO_DATE_TIME("2025-12-24T05-50-55_CET",
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
  MODIFIED_ISO_DATE_TIME("2025-12-24T05-50-55",
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
  BASIC_ISO_DATE("20251224", DateTimeFormatter.BASIC_ISO_DATE, "\\d{8}"), // 20241231 yyyyMMdd
  ISO_DATE("2025-12-24", DateTimeFormatter.ISO_DATE, "\\d{4}-\\d{2}-\\d{2}"), // yyyy-MM-dd
  EUROPEAN_DATE("24.12.2025", "dd.MM.yyyy", "\\d{2}\\.\\d{2}\\.\\d{4}"), //
  JAPANESE_DATE("2025.12.24", "yyyy.MM.dd", "\\d{4}\\.\\d{2}\\.\\d{2}"); //

  private final String example;
  private final DateTimeFormatter formatter;
  private final Pattern pattern;
  private final Pattern patternStarts;
  private final Pattern patternEnds;
  private final boolean timed;
  private final boolean zoned;

  LocalDateTimeParser(String example, String pattern, String regex) {
    this(example, DateTimeFormatter.ofPattern(pattern), regex);
  }

  LocalDateTimeParser(final String example, final DateTimeFormatter formatter, final String regex) {
    this(example, formatter, regex, false, false);
  }

  LocalDateTimeParser(String example, DateTimeFormatter formatter, String regex, boolean timed,
      boolean zoned) {
    this.example = example;
    this.formatter = formatter;
    this.pattern = Pattern.compile(regex + "(?!\\d)"); // disallow trailing numbers
    this.patternStarts = Pattern.compile("^" + regex + "(?!\\d)"); // disallow trailing numbers
    this.patternEnds = Pattern.compile(regex + "$");
    this.timed = timed;
    this.zoned = zoned;
  }

  /**
   * yyyy-MM-dd separated by T, _, or space from HH:mm with optional :ss and optional fraction of
   * seconds. A method and not a constant because enum constants cannot reference fields that are
   * declared after them.
   */
  private static @NotNull String isoDateTimeRegex() {
    return "\\d{4}-\\d{2}-\\d{2}[T_ ]\\d{2}:\\d{2}(?::\\d{2}(?:\\.\\d{1,9})?)?";
  }

  /**
   * Z, +02:00, or +0200 optionally separated by space or underscore; or a zone id / abbreviation
   * that requires a space or underscore separator; optionally followed by [Europe/Berlin].
   */
  private static @NotNull String zoneSuffixRegex() {
    return
        "(?:[ _]?(?:Z|[+-]\\d{2}:?\\d{2})|[ _](?:[A-Za-z]+/[A-Za-z_]+|[A-Za-z]{2,5}(?![A-Za-z])))"
            + "(?:\\[[A-Za-z_+\\-0-9/]+])?";
  }

  /**
   * Date and time part shared by {@link #ISO_DATE_TIME} and {@link #ZONED_ISO_DATE_TIME}. Seconds
   * and the fraction of seconds are optional and default to 0.
   */
  private static @NotNull DateTimeFormatterBuilder isoDateTimeBuilder() {
    return new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd")
        // date time separator: T, _ (path compatible), or a space
        .optionalStart().appendLiteral('T').optionalEnd() //
        .optionalStart().appendLiteral('_').optionalEnd() //
        .optionalStart().appendLiteral(' ').optionalEnd() //
        .appendPattern("HH:mm") //
        .optionalStart().appendLiteral(':').appendPattern("ss")
        .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd();
  }

  /**
   * {@link #isoDateTimeBuilder()} followed by an optional separator and a zone or offset. The
   * offsets are appended as separate optional sections because a single offset pattern cannot
   * accept both +02:00 and +0200.
   */
  private static @NotNull DateTimeFormatter zonedIsoDateTimeFormatter() {
    return isoDateTimeBuilder()
        // separator between time and zone, e.g., 2025-12-24 05:50:55 CET
        .optionalStart().appendLiteral('_').optionalEnd() //
        .optionalStart().appendLiteral(' ').optionalEnd() //
        .optionalStart().appendOffset("+HH:MM", "Z").optionalEnd() // +02:00 or Z
        .optionalStart().appendOffset("+HHMM", "Z").optionalEnd() // +0200
        .optionalStart().appendZoneOrOffsetId().optionalEnd() // CET, UTC, Europe/Berlin
        .optionalStart().appendLiteral('[').appendZoneRegionId().appendLiteral(']').optionalEnd()
        .toFormatter(Locale.ENGLISH);
  }

  public DateTimeFormatter getFormatter() {
    return formatter;
  }

  /**
   * @return an example string of this format, used to describe the accepted input to users
   */
  public @NotNull String getExample() {
    return example;
  }

  /**
   * @return all supported formats as examples, separated by comma, in the order they are tried
   */
  static @NotNull String supportedFormatExamples() {
    return Arrays.stream(values()).map(LocalDateTimeParser::getExample)
        .collect(Collectors.joining(", "));
  }

  public Pattern getPattern() {
    return pattern;
  }

  /**
   * @param input any string that contains a date pattern anywhere in the string
   * @return the local date or null if this format does not match
   * @throws DateTimeParseException if the matched text is no valid date
   */
  public @Nullable LocalDateTime parseFirst(final @NotNull String input) {
    return parseMatch(pattern.matcher(input));
  }

  /**
   * @param input any string that starts with a date pattern
   * @return the local date or null if this format does not match
   * @throws DateTimeParseException if the matched text is no valid date
   */
  public @Nullable LocalDateTime parseStart(final @NotNull String input) {
    return parseMatch(patternStarts.matcher(input));
  }

  /**
   * @param input any string that ends with a date pattern
   * @return the local date or null if this format does not match
   * @throws DateTimeParseException if the matched text is no valid date
   */
  public @Nullable LocalDateTime parseEnd(final @NotNull String input) {
    return parseMatch(patternEnds.matcher(input));
  }

  /**
   * @param matcher a matcher of one of the patterns of this format
   * @return the local date of the first match or null if there was no match
   * @throws DateTimeParseException if the matched text is no valid date
   */
  private @Nullable LocalDateTime parseMatch(final @NotNull Matcher matcher) {
    if (!matcher.find()) {
      return null;
    }
    final String match = matcher.group(0);
    if (zoned) {
      return DateTimeUtils.getStandardUtcLocalTime(ZonedDateTime.parse(match, formatter));
    }
    if (timed) {
      return LocalDateTime.parse(match, formatter);
    }
    return LocalDate.parse(match, formatter).atStartOfDay();
  }

  /**
   * @param input any string that contains a date pattern anywhere in the string
   * @return the local date or null if no format matched
   */
  static @Nullable LocalDateTime parseAnyFirstDate(final @NotNull String input) {
    for (final LocalDateTimeParser parser : values()) {
      try {
        final LocalDateTime date = parser.parseFirst(input);
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
   * @param input any string that starts with a date pattern
   * @return the local date or null if no format matched
   */
  static @Nullable LocalDateTime parseAnyStartingDate(final @NotNull String input) {
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
   * @return the local date or null if no format matched
   */
  static @Nullable LocalDateTime parseAnyEndingDate(final @NotNull String input) {
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
