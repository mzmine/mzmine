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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class LocalDateTimeParserTest {

  @Test
  void shiftTimeZone() {
    ZonedDateTime zoned = ZonedDateTime.of(2025, 4, 24, 10, 30, 0, 0, ZoneId.of("+05:00"));
    // this shifts the internal LocalDateTime to the actual standard time
    ZonedDateTime utcTime = zoned.withZoneSameInstant(ZoneId.of("UTC"));
    // this just removes the zone
    LocalDateTime correctedLocalTime = utcTime.toLocalDateTime();
    assertEquals(LocalDateTime.of(2025, 4, 24, 5, 30, 0), correctedLocalTime);
  }

  @Test
  void parseDateTime() {
    final Pattern pattern = LocalDateTimeParser.MODIFIED_ISO_DATE_TIME.getPattern();
    final Matcher matcher = pattern.matcher("2020-05-20_08-50-15");
    assertTrue(matcher.find());
    assertNotNull(matcher.group(0));

    assertEquals(LocalDateTime.of(2020, 5, 20, 8, 50, 15),
        LocalDateTimeParser.parseAnyFirstDate("2020-05-20_08-50-15"));
    assertEquals(LocalDateTime.of(2020, 5, 20, 8, 50, 15),
        LocalDateTimeParser.parseAnyFirstDate("2020-05-20_08-50-15adjwkfajal"));
    assertEquals(LocalDateTime.of(2020, 5, 20, 8, 50, 15),
        LocalDateTimeParser.parseAnyFirstDate("dawfahwsldk2020-05-20_08-50-15adjwkfajal"));
    assertEquals(LocalDateTime.of(2020, 5, 20, 8, 50, 15),
        LocalDateTimeParser.parseAnyFirstDate("dawfahwsldk2020-05-20_08-50-15_5adjwkfajal"));
    assertEquals(LocalDateTime.of(2020, 5, 20, 8, 50, 15),
        LocalDateTimeParser.parseAnyFirstDate("2020-05-20T08-50-15"));
    assertEquals(LocalDateTime.of(2020, 5, 20, 8, 50, 15),
        LocalDateTimeParser.parseAnyFirstDate("2020-05-20T08-50-15"));
    final LocalDateTime CET = LocalDateTimeParser.parseAnyFirstDate("2020-05-20T08-50-15_CET");
    assertEquals(LocalDateTime.of(2020, 5, 20, 6, 50, 15), CET);
    assertEquals(LocalDateTime.of(2020, 5, 20, 6, 50, 15),
        LocalDateTimeParser.parseAnyFirstDate("2020-05-20T08-50-15_+0200"));

  }

  @Test
  void parseAnyDate() {
    assertEquals(LocalDate.of(2020, 5, 20),
        LocalDateTimeParser.parseAnyFirstDate("2020-05-20").toLocalDate());
    // always at start of day if just date. This is for checks that check if something ran after that date
    assertEquals(LocalDateTime.of(2020, 5, 20, 0, 0, 0),
        LocalDateTimeParser.parseAnyFirstDate("20200520"));
    assertEquals(LocalDate.of(2020, 5, 20),
        LocalDateTimeParser.parseAnyFirstDate("2020.05.20").toLocalDate());
    assertEquals(LocalDate.of(2020, 5, 20),
        LocalDateTimeParser.parseAnyFirstDate("2020-05-20_somthing foll3owing").toLocalDate());
    assertEquals(LocalDate.of(2020, 5, 20),
        LocalDateTimeParser.parseAnyFirstDate("Lead3ing2020-05-20").toLocalDate());
    assertEquals(LocalDate.of(2020, 5, 20),
        LocalDateTimeParser.parseAnyFirstDate("Lead3ing2020-05-20_and trai3ling").toLocalDate());
    assertEquals(LocalDate.of(2020, 5, 20),
        LocalDateTimeParser.parseAnyFirstDate("Lead3ing20200520_and trai3ling").toLocalDate());
    assertEquals(LocalDate.of(2020, 5, 20),
        LocalDateTimeParser.parseAnyFirstDate("Lead3ing20200520").toLocalDate());
    assertEquals(LocalDate.of(2020, 5, 20),
        LocalDateTimeParser.parseAnyFirstDate("20200520_trail3ing").toLocalDate());
    assertNull(LocalDateTimeParser.parseAnyFirstDate("2020052034234235_some num4bers"));
    assertNull(LocalDateTimeParser.parseAnyFirstDate("2020-05-2034234235_some num4bers"));
  }

  @Test
  void parseEndingDate() {
    assertEquals(LocalDateTime.of(2020, 5, 20, 0, 0, 0),
        LocalDateTimeParser.parseAnyEndingDate("2020-05-20"));
    assertEquals(LocalDateTime.of(2020, 5, 20, 0, 0, 0),
        LocalDateTimeParser.parseAnyEndingDate("20200520"));
    assertEquals(LocalDateTime.of(2020, 5, 20, 0, 0, 0),
        LocalDateTimeParser.parseAnyEndingDate("2020.05.20"));
    assertEquals(LocalDateTime.of(2020, 5, 20, 0, 0, 0),
        LocalDateTimeParser.parseAnyEndingDate("Leading2020-05-20"));
    assertEquals(LocalDateTime.of(2020, 5, 20, 0, 0, 0),
        LocalDateTimeParser.parseAnyEndingDate("Leading20200520"));

    assertNull(LocalDateTimeParser.parseAnyEndingDate("2020-05-20_somthing1 following"));
    assertNull(LocalDateTimeParser.parseAnyEndingDate("Lead4ing2020-05-20_and t2railing"));
    assertNull(LocalDateTimeParser.parseAnyEndingDate("Lead3ing20200520_and tra3iling"));
    assertNull(LocalDateTimeParser.parseAnyEndingDate("20200520_trailing"));
    assertNull(LocalDateTimeParser.parseAnyEndingDate("2020052034234235_some nu23mbers"));
    assertNull(LocalDateTimeParser.parseAnyEndingDate("2020-05-2034234235_some nu1mbers"));
  }

  @Test
  void parseStartingDate() {
    assertEquals(LocalDateTime.of(2020, 5, 20, 0, 0, 0),
        LocalDateTimeParser.parseAnyStartingDate("2020-05-20"));
    assertEquals(LocalDateTime.of(2020, 5, 20, 0, 0, 0),
        LocalDateTimeParser.parseAnyStartingDate("20200520"));
    assertEquals(LocalDateTime.of(2020, 5, 20, 0, 0, 0),
        LocalDateTimeParser.parseAnyStartingDate("2020.05.20"));
    assertEquals(LocalDateTime.of(2020, 5, 20, 0, 0, 0),
        LocalDateTimeParser.parseAnyStartingDate("2020-05-20_somthi4ng following"));
    assertEquals(LocalDateTime.of(2020, 5, 20, 0, 0, 0),
        LocalDateTimeParser.parseAnyStartingDate("20200520_trai2ling"));
    assertNull(LocalDateTimeParser.parseAnyStartingDate("Le3ading2020-05-20"));
    assertNull(LocalDateTimeParser.parseAnyStartingDate("Le4ading2020-05-20_and tra4iling"));
    assertNull(LocalDateTimeParser.parseAnyStartingDate("Lea4ding20200520_and tra4iling"));
    assertNull(LocalDateTimeParser.parseAnyStartingDate("Le4ading20200520"));
    assertNull(LocalDateTimeParser.parseAnyStartingDate("2020052034234235_some num4bers"));
    assertNull(LocalDateTimeParser.parseAnyStartingDate("2020-05-2034234235_some nu4mbers"));
  }
}