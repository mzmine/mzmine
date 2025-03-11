package io.github.mzmine.util.date;

import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class LocalDateParserTest {

  @Test
  void parseAnyDate() {
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyFirstDate("2020-05-20"));
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyFirstDate("20200520"));
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyFirstDate("2020.05.20"));
    assertEquals(LocalDate.of(2020, 5, 20),
        LocalDateParser.parseAnyFirstDate("2020-05-20_somthing foll3owing"));
    assertEquals(LocalDate.of(2020, 5, 20),
        LocalDateParser.parseAnyFirstDate("Lead3ing2020-05-20"));
    assertEquals(LocalDate.of(2020, 5, 20),
        LocalDateParser.parseAnyFirstDate("Lead3ing2020-05-20_and trai3ling"));
    assertEquals(LocalDate.of(2020, 5, 20),
        LocalDateParser.parseAnyFirstDate("Lead3ing20200520_and trai3ling"));
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyFirstDate("Lead3ing20200520"));
    assertEquals(LocalDate.of(2020, 5, 20),
        LocalDateParser.parseAnyFirstDate("20200520_trail3ing"));
    assertNull(LocalDateParser.parseAnyFirstDate("2020052034234235_some num4bers"));
    assertNull(LocalDateParser.parseAnyFirstDate("2020-05-2034234235_some num4bers"));
  }

  @Test
  void parseEndingDate() {
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyEndingDate("2020-05-20"));
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyEndingDate("20200520"));
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyEndingDate("2020.05.20"));
    assertEquals(LocalDate.of(2020, 5, 20),
        LocalDateParser.parseAnyEndingDate("Leading2020-05-20"));
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyEndingDate("Leading20200520"));

    assertNull(LocalDateParser.parseAnyEndingDate("2020-05-20_somthing1 following"));
    assertNull(LocalDateParser.parseAnyEndingDate("Lead4ing2020-05-20_and t2railing"));
    assertNull(LocalDateParser.parseAnyEndingDate("Lead3ing20200520_and tra3iling"));
    assertNull(LocalDateParser.parseAnyEndingDate("20200520_trailing"));
    assertNull(LocalDateParser.parseAnyEndingDate("2020052034234235_some nu23mbers"));
    assertNull(LocalDateParser.parseAnyEndingDate("2020-05-2034234235_some nu1mbers"));
  }

  @Test
  void parseStartingDate() {
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyStartingDate("2020-05-20"));
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyStartingDate("20200520"));
    assertEquals(LocalDate.of(2020, 5, 20), LocalDateParser.parseAnyStartingDate("2020.05.20"));
    assertEquals(LocalDate.of(2020, 5, 20),
        LocalDateParser.parseAnyStartingDate("2020-05-20_somthi4ng following"));
    assertEquals(LocalDate.of(2020, 5, 20),
        LocalDateParser.parseAnyStartingDate("20200520_trai2ling"));
    assertNull(LocalDateParser.parseAnyStartingDate("Le3ading2020-05-20"));
    assertNull(LocalDateParser.parseAnyStartingDate("Le4ading2020-05-20_and tra4iling"));
    assertNull(LocalDateParser.parseAnyStartingDate("Lea4ding20200520_and tra4iling"));
    assertNull(LocalDateParser.parseAnyStartingDate("Le4ading20200520"));
    assertNull(LocalDateParser.parseAnyStartingDate("2020052034234235_some num4bers"));
    assertNull(LocalDateParser.parseAnyStartingDate("2020-05-2034234235_some nu4mbers"));
  }
}