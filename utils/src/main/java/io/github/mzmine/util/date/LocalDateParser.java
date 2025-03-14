package io.github.mzmine.util.date;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/**
 * Currently this only supports date formats valid in file paths. US format with / slash is not a
 * compatible character for paths.
 */
public enum LocalDateParser {
  // uses (?!\d) as look ahead to make date followed by another number illegal.
  BASIC_ISO_DATE(DateTimeFormatter.BASIC_ISO_DATE, "\\d{8}"), // 20241231 yyyyMMdd
  ISO_DATE(DateTimeFormatter.ISO_DATE, "\\d{4}-\\d{2}-\\d{2}"), // yyyy-MM-dd
  EUROPEAN_DATE("dd.MM.yyyy", "\\d{2}\\.\\d{2}\\.\\d{4}"), //
  JAPANESE_DATE("yyyy.MM.dd", "\\d{4}\\.\\d{2}\\.\\d{2}"); //

  private final DateTimeFormatter formatter;
  private final Pattern pattern;
  private final Pattern patternStarts;
  private final Pattern patternEnds;

  LocalDateParser(String pattern, String regex) {
    this(DateTimeFormatter.ofPattern(pattern), regex);
  }

  LocalDateParser(final DateTimeFormatter formatter, final String regex) {
    this.formatter = formatter;
    this.pattern = Pattern.compile(regex + "(?!\\d)"); // disallow trailing numbers
    this.patternStarts = Pattern.compile("^" + regex + "(?!\\d)"); // disallow trailing numbers
    this.patternEnds = Pattern.compile(regex + "$");
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
  public @Nullable LocalDate parseFirst(String input) {
    final Matcher matcher = pattern.matcher(input);
    if (matcher.find()) {
      return LocalDate.parse(matcher.group(0), formatter);
    }
    return null;
  }

  /**
   * @param input any string that starts with a date pattern
   * @return the local date
   * @throws DateTimeParseException
   */
  public @Nullable LocalDate parseStart(String input) {
    final Matcher matcher = patternStarts.matcher(input);
    if (matcher.find()) {
      return LocalDate.parse(matcher.group(0), formatter);
    }
    return null;
  }

  /**
   * @param input any string that ends with a date pattern
   * @return the local date
   * @throws DateTimeParseException
   */
  public @Nullable LocalDate parseEnd(String input) {
    final Matcher matcher = patternEnds.matcher(input);
    if (matcher.find()) {
      return LocalDate.parse(matcher.group(0), formatter);
    }
    return null;
  }

  /**
   * @param input any string that contains a date pattern anywhere in the string
   * @return the local date
   */
  public static @Nullable LocalDate parseAnyFirstDate(String input) {
    for (final LocalDateParser parser : values()) {
      try {
        final LocalDate date = parser.parseFirst(input);
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
   * @return the local date
   */
  public static @Nullable LocalDate parseAnyStartingDate(String input) {
    for (final LocalDateParser parser : values()) {
      try {
        final LocalDate date = parser.parseStart(input);
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
  public static @Nullable LocalDate parseAnyEndingDate(String input) {
    for (final LocalDateParser parser : values()) {
      try {
        final LocalDate date = parser.parseEnd(input);
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