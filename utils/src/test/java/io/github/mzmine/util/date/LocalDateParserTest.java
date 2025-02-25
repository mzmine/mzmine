package io.github.mzmine.util.date;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LocalDateParserTest {

  @Test
  void parseAnyDate() {
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyDate("2020-05-20"));
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyDate("20200520"));
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyDate("2020.05.20"));
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyDate("2020-05-20_somthing following"));
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyDate("Leading2020-05-20"));
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyDate("Leading2020-05-20_and trailing"));
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyDate("Leading20200520_and trailing"));
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyDate("Leading20200520"));
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyDate("20200520_trailing"));
    assertNull(LocalDateParser.parseAnyDate("2020052034234235_some numbers"));
    assertNull(LocalDateParser.parseAnyDate("2020-05-2034234235_some numbers"));
  }
}