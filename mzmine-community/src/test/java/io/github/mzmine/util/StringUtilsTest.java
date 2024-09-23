/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StringUtilsTest {

  @Test
  void parseIntegerOrElse() {
    assertEquals(123, StringUtils.parseIntegerOrElse("+123", true, 5));
    assertEquals(123, StringUtils.parseIntegerOrElse("123", true, 5));
    assertEquals(123, StringUtils.parseIntegerOrElse("dahklwdhla123dwahjdwhakd", true, 5));
    assertEquals(123, StringUtils.parseIntegerOrElse("dahklwdhla-123dwahjdwhakd", true, 5));
    assertEquals(123, StringUtils.parseIntegerOrElse("dahk-1lwdhla23dwahjdwhakd", true, 5));
    assertEquals(5, StringUtils.parseIntegerOrElse("dahklwdhladwahjdwhakd", true, 5));
    assertEquals(5, StringUtils.parseIntegerOrElse(null, true, 5));
  }

  @Test
  void parseIntegerPrefixOrElse() {
    assertEquals(123, StringUtils.parseIntegerPrefixOrElse("+123", 5));
    assertEquals(123, StringUtils.parseIntegerPrefixOrElse("123", 5));
    assertEquals(123, StringUtils.parseIntegerPrefixOrElse("123dahklwdhla123dwahjdwhakd", 5));
    assertEquals(-123, StringUtils.parseIntegerPrefixOrElse("-123dahklwdhla-123dwahjdwhakd", 5));
    assertEquals(5, StringUtils.parseIntegerPrefixOrElse("dahklwdhladwahjdwhakd", 5));
    assertEquals(5, StringUtils.parseIntegerPrefixOrElse(null, 5));
  }

  @Test
  void removeIntegerPrefix() {
  }

  @Test
  void parseSignAndIntegerOrElse() {
    assertEquals(123, StringUtils.parseSignAndIntegerOrElse("+123", true, 5));
    assertEquals(123, StringUtils.parseSignAndIntegerOrElse("123", true, 5));
    assertEquals(123, StringUtils.parseSignAndIntegerOrElse("dahklwdhla123dwahjdwhakd", true, 5));
    assertEquals(-123, StringUtils.parseSignAndIntegerOrElse("dahklwdhla-123dwahjdwhakd", true, 5));
    assertEquals(-123, StringUtils.parseSignAndIntegerOrElse("dahk-1lwdhla23dwahjdwhakd", true, 5));
    assertEquals(5, StringUtils.parseSignAndIntegerOrElse("dahklwdhladwahjdwhakd", true, 5));
    assertEquals(5, StringUtils.parseSignAndIntegerOrElse(null, true, 5));
    assertEquals(3, StringUtils.parseSignAndIntegerOrElse("+3", true, 5));
    assertEquals(3, StringUtils.parseSignAndIntegerOrElse("3+", true, 5));
  }

  @Test
  void getDigits() {
    assertEquals("123", StringUtils.getDigits("+123"));
    assertEquals("123", StringUtils.getDigits("123"));
    assertEquals("123", StringUtils.getDigits("dahklwdhla123dwahjdwhakd"));
    assertEquals("123", StringUtils.getDigits("dahklwdhla-123dwahjdwhakd"));
    assertEquals("123", StringUtils.getDigits("dahk-1lwdhla23dwahjdwhakd"));
    assertEquals("", StringUtils.getDigits("dahklwdhladwahjdwhakd"));
    assertEquals("", StringUtils.getDigits(null));
    assertEquals("3", StringUtils.getDigits("+3"));
    assertEquals("3", StringUtils.getDigits("3+"));
  }

  @Test
  void orDefault() {
    assertEquals("default", StringUtils.orDefault("", "default"));
    assertEquals("default", StringUtils.orDefault(" ", "default"));
    assertEquals("default", StringUtils.orDefault("\t", "default"));
    assertEquals("default", StringUtils.orDefault(null, "default"));
    assertEquals("value", StringUtils.orDefault("value", "default"));
  }

  @Test
  void splitAnyCommaTabSpace() {
    String[] s = StringUtils.splitAnyCommaTabSpace("Hello,mzmine\t well, done ! ");
    assertEquals(6, s.length);
    assertEquals("Hello", s[0]);
    assertEquals("mzmine", s[1]);
    assertEquals("", s[2]);
    assertEquals("well", s[3]);
    assertEquals("done", s[4]);
    assertEquals("!", s[5]);

    String input = "No\nsplit";
    var noSplit = StringUtils.splitAnyCommaTabSpace(input);
    assertEquals(1, noSplit.length);
    assertEquals(input, noSplit[0]);
  }
}