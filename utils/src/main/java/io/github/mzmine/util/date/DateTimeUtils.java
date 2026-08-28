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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Single entry point to parse and format dates. MZmine uses {@link LocalDateTime} to represent date
 * + time in this format: 2022-06-01T18:36:09
 * <p>
 * 2022-06-01T18:36:09Z is a zoned format that is converted to UTC and then to
 * {@link LocalDateTime}.
 * <p>
 * {@link #parse(String)} requires the whole input to be a date. Use
 * {@link #parseAnyFirstDate(String)}, {@link #parseAnyStartingDate(String)}, or
 * {@link #parseAnyEndingDate(String)} to extract a date from a longer string like a file name. The
 * supported formats are listed in the package private {@link LocalDateTimeParser}.
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class DateTimeUtils {

  /**
   * Path compatible variant of {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}, e.g.,
   * 2025-12-24T05-50-55, because colons are illegal in file paths. There is no JDK constant for
   * this. Seconds are always printed and the fraction of seconds is dropped on purpose to keep file
   * names a fixed length. uuuu instead of yyyy to use the proleptic year like the JDK constants.
   */
  private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern(
      "uuuu-MM-dd'T'HH-mm-ss", Locale.ENGLISH);

  private DateTimeUtils() {
  }

  /**
   * Obtains an instance of LocalDateTime from a text string such as 2007-12-03T10:15:30. The string
   * must represent a valid date-time and is parsed using DateTimeFormatter.ISO_LOCAL_DATE_TIME.
   *
   * @param dateTime the text to parse such as "2007-12-03T10:15:30" or "2007-12-03T10:15:30Z"
   * @return the parsed local date-time, not null
   * @throws DateTimeParseException   – if the text cannot be parsed
   * @throws IllegalArgumentException if text cannot be parsed by
   *                                  {@link LocalDateTimeParser#parseAnyFirstDate(String)}
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
        final LocalDateTime parsed = LocalDateTimeParser.parseAnyFirstDate(dateTime);
        if (parsed == null) {
          throw new IllegalArgumentException("Could not parse date: " + dateTime);
        }
        return parsed;
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

  /**
   * Searches the input for the first date or date time of any supported format, e.g., to extract a
   * date from a file name.
   *
   * @param input any string that contains a date pattern anywhere in the string
   * @return the parsed local date-time or null if no format matched
   */
  public static @Nullable LocalDateTime parseAnyFirstDate(final @NotNull String input) {
    return LocalDateTimeParser.parseAnyFirstDate(input);
  }

  /**
   * @param input        any string that contains a date pattern anywhere in the string
   * @param defaultValue returned if input was null or contained no date
   * @return the parsed local date-time or the default value
   */
  public static @Nullable LocalDateTime parseAnyFirstDateOrElse(final @Nullable String input,
      final @Nullable LocalDateTime defaultValue) {
    return parseAnyOrElse(input, defaultValue, LocalDateTimeParser::parseAnyFirstDate);
  }

  /**
   * @param input any string that starts with a date pattern
   * @return the parsed local date-time or null if no format matched
   */
  public static @Nullable LocalDateTime parseAnyStartingDate(final @NotNull String input) {
    return LocalDateTimeParser.parseAnyStartingDate(input);
  }

  /**
   * @param input        any string that starts with a date pattern
   * @param defaultValue returned if input was null or started with no date
   * @return the parsed local date-time or the default value
   */
  public static @Nullable LocalDateTime parseAnyStartingDateOrElse(final @Nullable String input,
      final @Nullable LocalDateTime defaultValue) {
    return parseAnyOrElse(input, defaultValue, LocalDateTimeParser::parseAnyStartingDate);
  }

  /**
   * @param input any string that ends with a date pattern
   * @return the parsed local date-time or null if no format matched
   */
  public static @Nullable LocalDateTime parseAnyEndingDate(final @NotNull String input) {
    return LocalDateTimeParser.parseAnyEndingDate(input);
  }

  /**
   * @param input        any string that ends with a date pattern
   * @param defaultValue returned if input was null or ended with no date
   * @return the parsed local date-time or the default value
   */
  public static @Nullable LocalDateTime parseAnyEndingDateOrElse(final @Nullable String input,
      final @Nullable LocalDateTime defaultValue) {
    return parseAnyOrElse(input, defaultValue, LocalDateTimeParser::parseAnyEndingDate);
  }

  private static @Nullable LocalDateTime parseAnyOrElse(final @Nullable String input,
      final @Nullable LocalDateTime defaultValue,
      final @NotNull Function<String, LocalDateTime> parser) {
    if (input == null) {
      return defaultValue;
    }
    final LocalDateTime parsed = parser.apply(input);
    return parsed == null ? defaultValue : parsed;
  }

  /**
   * Like {@link #parse(String)} but without exceptions, for call sites that check for a value
   * instead of handling an error.
   *
   * @param dateTime the text to parse, the whole text needs to be a date
   * @return the parsed local date-time or empty if input was null or no format matched
   */
  public static @NotNull Optional<LocalDateTime> tryParse(final @Nullable String dateTime) {
    return Optional.ofNullable(parseOrElse(dateTime, null));
  }

  /**
   * Like {@link #parseAnyFirstDate(String)} but without null handling at the call site.
   *
   * @param input any string that contains a date pattern anywhere in the string
   * @return the parsed local date-time or empty if input was null or no format matched
   */
  public static @NotNull Optional<LocalDateTime> tryParseAnyFirstDate(
      final @Nullable String input) {
    return Optional.ofNullable(parseAnyFirstDateOrElse(input, null));
  }

  /**
   * Parses user input and reports the accepted formats if it fails. Use this for values typed into
   * dialogs or read from user editable files.
   *
   * @param dateTime  the text to parse, the whole text needs to be a date
   * @param fieldName name of the parameter or column, used in the error message
   * @return the parsed local date-time, never null
   * @throws IllegalArgumentException if the input is null, blank, or in no supported format
   */
  public static @NotNull LocalDateTime parseOrThrow(final @Nullable String dateTime,
      final @NotNull String fieldName) {
    if (dateTime == null || dateTime.isBlank()) {
      throw new IllegalArgumentException(
          "Missing date for %s. Supported formats are: %s".formatted(fieldName,
              LocalDateTimeParser.supportedFormatExamples()));
    }
    final LocalDateTime parsed = parseOrElse(dateTime, null);
    if (parsed == null) {
      throw new IllegalArgumentException(
          "Cannot parse date \"%s\" for %s. Supported formats are: %s".formatted(dateTime,
              fieldName, LocalDateTimeParser.supportedFormatExamples()));
    }
    return parsed;
  }

  /**
   * The canonical mzmine format. Contrary to {@link LocalDateTime#toString()} the seconds are
   * always printed, a fraction of seconds only if there is one.
   *
   * @param dateTime a local date time
   * @return e.g., 2025-12-24T05:50:55 or 2025-12-24T05:50:55.123
   */
  public static @NotNull String format(final @NotNull LocalDateTime dateTime) {
    return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime);
  }

  /**
   * Path compatible format without colons, which are illegal in file paths. The result is parsed
   * back by {@link #parse(String)} and the parseAny methods.
   *
   * @param dateTime a local date time
   * @return e.g., 2025-12-24T05-50-55
   */
  public static @NotNull String formatForFileName(final @NotNull LocalDateTime dateTime) {
    return FILE_NAME_FORMATTER.format(dateTime);
  }

  /**
   * @param dateTime a zoned date time, the zone is printed as offset and not converted to UTC
   * @return e.g., 2025-12-24T05:50:55+02:00 or 2025-12-24T05:50:55Z
   */
  public static @NotNull String formatIso(final @NotNull ZonedDateTime dateTime) {
    return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime);
  }

  /**
   * @param epochMillis milliseconds since 1970-01-01T00:00:00Z
   * @return the UTC local date-time of this instant
   */
  public static @NotNull LocalDateTime fromEpochMillis(final long epochMillis) {
    return fromInstant(Instant.ofEpochMilli(epochMillis));
  }

  /**
   * @param dateTime a local date-time that is interpreted as UTC
   * @return milliseconds since 1970-01-01T00:00:00Z
   */
  public static long toEpochMillis(final @NotNull LocalDateTime dateTime) {
    return toInstant(dateTime).toEpochMilli();
  }

  /**
   * @param instant any instant
   * @return the UTC local date-time of this instant
   */
  public static @NotNull LocalDateTime fromInstant(final @NotNull Instant instant) {
    return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  /**
   * @param dateTime a local date-time that is interpreted as UTC
   * @return the instant of this date-time
   */
  public static @NotNull Instant toInstant(final @NotNull LocalDateTime dateTime) {
    return dateTime.toInstant(ZoneOffset.UTC);
  }

  /**
   * Inclusive range check with optional open ends, e.g., to filter raw data files acquired in a
   * time frame.
   *
   * @param dateTime the value to test
   * @param from     the inclusive lower bound or null for no lower bound
   * @param to       the inclusive upper bound or null for no upper bound
   * @return true if the date-time is within the bounds
   */
  public static boolean isBetween(final @NotNull LocalDateTime dateTime,
      final @Nullable LocalDateTime from, final @Nullable LocalDateTime to) {
    return (from == null || !dateTime.isBefore(from)) && (to == null || !dateTime.isAfter(to));
  }

  /**
   * @param dateTimes any collection, null elements are skipped
   * @return the earliest date-time or null if there was none
   */
  public static @Nullable LocalDateTime earliest(
      final @Nullable Collection<@Nullable LocalDateTime> dateTimes) {
    return minMax(dateTimes, true);
  }

  /**
   * @param dateTimes any collection, null elements are skipped
   * @return the latest date-time or null if there was none
   */
  public static @Nullable LocalDateTime latest(
      final @Nullable Collection<@Nullable LocalDateTime> dateTimes) {
    return minMax(dateTimes, false);
  }

  private static @Nullable LocalDateTime minMax(
      final @Nullable Collection<@Nullable LocalDateTime> dateTimes, final boolean earliest) {
    if (dateTimes == null) {
      return null;
    }
    final Stream<LocalDateTime> stream = dateTimes.stream().filter(Objects::nonNull);
    final Comparator<LocalDateTime> comparator = Comparator.naturalOrder();
    return (earliest ? stream.min(comparator) : stream.max(comparator)).orElse(null);
  }

}
