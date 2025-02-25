package io.github.mzmine.util.date;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Currently this only supports date formats valid in file paths. US format with / slash is not a
 * compatible character for paths.
 */
public enum LocalDateParser {
  // uses (?!\d) as look ahead to make date followed by another number illegal.
  BASIC_ISO_DATE(DateTimeFormatter.BASIC_ISO_DATE, "\\d{8}(?!\\d)"), // 20241231 yyyyMMdd
  ISO_DATE(DateTimeFormatter.ISO_DATE, "\\d{4}-\\d{2}-\\d{2}(?!\\d)"), // yyyy-MM-dd
  EUROPEAN_DATE("dd.MM.yyyy", "\\d{2}\\.\\d{2}\\.\\d{4}(?!\\d)"), //
  EUROPEAN_DATE_REVERSED("yyyy.MM.dd", "\\d{4}\\.\\d{2}\\.\\d{2}(?!\\d)");

  private final DateTimeFormatter formatter;
  private final Pattern pattern;

  LocalDateParser(String pattern, String regex) {
    this(DateTimeFormatter.ofPattern(pattern), regex);
  }

  LocalDateParser(final DateTimeFormatter formatter, final String regex) {
    this.formatter = formatter;
    this.pattern = Pattern.compile(regex);
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
  public @NotNull LocalDate parse(String input) {
    final Matcher matcher = pattern.matcher(input);
    if (matcher.find()) {
      return LocalDate.parse(matcher.group(0), formatter);
    }
    throw new DateTimeParseException("Could not parse date: " + input, input, 0);
  }

  /**
   * @param input any string that contains a date pattern anywhere in the string
   * @return the local date
   */
  public static @Nullable LocalDate parseAnyDate(String input) {
    for (final LocalDateParser parser : values()) {
      try {
        return parser.parse(input);
      } catch (DateTimeParseException ex) {
        // silent and try next
      }
    }
    return null;
  }

}