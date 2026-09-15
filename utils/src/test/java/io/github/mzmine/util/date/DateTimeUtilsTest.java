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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DateTimeUtilsTest {

  private static final LocalDateTime DATE = LocalDateTime.of(2025, 12, 24, 5, 50, 55);

  @Test
  void format() {
    assertEquals("2025-12-24T05:50:55", DateTimeUtils.format(DATE));
    // seconds are always printed, contrary to LocalDateTime#toString
    assertEquals("2025-12-24T05:50:00",
        DateTimeUtils.format(LocalDateTime.of(2025, 12, 24, 5, 50)));
    // sub second precision is kept and can be parsed back
    final LocalDateTime withNanos = DATE.withNano(123_000_000);
    assertEquals("2025-12-24T05:50:55.123", DateTimeUtils.format(withNanos));
    assertEquals(withNanos, DateTimeUtils.parse(DateTimeUtils.format(withNanos)));
  }

  @Test
  void formatForFileName() {
    final String formatted = DateTimeUtils.formatForFileName(DATE);
    assertEquals("2025-12-24T05-50-55", formatted);
    assertFalse(formatted.contains(":"));
    // fixed length, a fraction of seconds is dropped in file names
    assertEquals(formatted, DateTimeUtils.formatForFileName(DATE.withNano(123_000_000)));
    // needs to round trip through the parser
    assertEquals(DATE, DateTimeUtils.parse(formatted));
    assertEquals(DATE, DateTimeUtils.parseAnyFirstDate("blank_" + formatted + ".mzML"));
  }

  @Test
  void formatIso() {
    assertEquals("2025-12-24T05:50:55+02:00",
        DateTimeUtils.formatIso(DATE.atZone(ZoneOffset.ofHours(2))));
    assertEquals("2025-12-24T05:50:55Z", DateTimeUtils.formatIso(DATE.atZone(ZoneOffset.UTC)));
    // a zone region is printed as its offset, sub second precision is kept
    assertEquals("2025-12-24T05:50:55.123+01:00",
        DateTimeUtils.formatIso(DATE.withNano(123_000_000).atZone(ZoneId.of("Europe/Berlin"))));
    // round trip, the offset is shifted to UTC on parsing
    final ZonedDateTime zoned = DATE.atZone(ZoneOffset.ofHours(2));
    assertEquals(DateTimeUtils.getStandardUtcLocalTime(zoned),
        DateTimeUtils.parse(DateTimeUtils.formatIso(zoned)));
  }

  @Test
  void tryParse() {
    assertEquals(Optional.of(DATE), DateTimeUtils.tryParse("2025-12-24T05:50:55"));
    assertEquals(Optional.empty(), DateTimeUtils.tryParse(null));
    assertEquals(Optional.empty(), DateTimeUtils.tryParse("no date at all"));

    assertEquals(Optional.of(DATE),
        DateTimeUtils.tryParseAnyFirstDate("blank_2025-12-24T05-50-55.mzML"));
    assertEquals(Optional.empty(), DateTimeUtils.tryParseAnyFirstDate(null));
    assertEquals(Optional.empty(), DateTimeUtils.tryParseAnyFirstDate("blank.mzML"));
  }

  @Test
  void parseOrThrow() {
    assertEquals(DATE, DateTimeUtils.parseOrThrow("2025-12-24T05:50:55", "acquisition date"));

    final IllegalArgumentException blank = assertThrows(IllegalArgumentException.class,
        () -> DateTimeUtils.parseOrThrow("  ", "acquisition date"));
    assertTrue(blank.getMessage().contains("acquisition date"));

    final IllegalArgumentException invalid = assertThrows(IllegalArgumentException.class,
        () -> DateTimeUtils.parseOrThrow("24/12/2025", "acquisition date"));
    // the message needs to name the field and list the accepted formats
    assertTrue(invalid.getMessage().contains("acquisition date"));
    assertTrue(invalid.getMessage().contains("2025-12-24T05:50:55"));
    assertTrue(invalid.getMessage().contains("24.12.2025"));
  }

  @Test
  void epochAndInstant() {
    final long epochMillis = 1766555455000L; // 2025-12-24T05:50:55Z
    assertEquals(DATE, DateTimeUtils.fromEpochMillis(epochMillis));
    assertEquals(epochMillis, DateTimeUtils.toEpochMillis(DATE));

    final Instant instant = Instant.ofEpochMilli(epochMillis);
    assertEquals(DATE, DateTimeUtils.fromInstant(instant));
    assertEquals(instant, DateTimeUtils.toInstant(DATE));
    // round trip
    assertEquals(DATE, DateTimeUtils.fromEpochMillis(DateTimeUtils.toEpochMillis(DATE)));
  }

  @Test
  void isBetween() {
    final LocalDateTime before = DATE.minusDays(1);
    final LocalDateTime after = DATE.plusDays(1);

    assertTrue(DateTimeUtils.isBetween(DATE, before, after));
    // bounds are inclusive
    assertTrue(DateTimeUtils.isBetween(DATE, DATE, DATE));
    assertFalse(DateTimeUtils.isBetween(before, DATE, after));
    assertFalse(DateTimeUtils.isBetween(after, before, DATE));
    // null bounds are open ends
    assertTrue(DateTimeUtils.isBetween(DATE, null, after));
    assertTrue(DateTimeUtils.isBetween(DATE, before, null));
    assertTrue(DateTimeUtils.isBetween(DATE, null, null));
    assertFalse(DateTimeUtils.isBetween(DATE, after, null));
  }

  @Test
  void earliestAndLatest() {
    final LocalDateTime before = DATE.minusDays(1);
    final LocalDateTime after = DATE.plusDays(1);
    final List<LocalDateTime> dates = Arrays.asList(DATE, null, after, before);

    assertEquals(before, DateTimeUtils.earliest(dates));
    assertEquals(after, DateTimeUtils.latest(dates));
    assertNull(DateTimeUtils.earliest(List.of()));
    assertNull(DateTimeUtils.latest(List.of()));
    assertNull(DateTimeUtils.earliest(null));
    assertNull(DateTimeUtils.latest(Arrays.asList(null, null)));
  }
}
