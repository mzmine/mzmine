/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StringUtilsTest {

  @Test
  void parseIntegerOrElse() {
    Assertions.assertEquals(123, StringUtils.parseIntegerOrElse("+123", true, 5));
    Assertions.assertEquals(123, StringUtils.parseIntegerOrElse("123", true, 5));
    Assertions.assertEquals(123, StringUtils.parseIntegerOrElse("dahklwdhla123dwahjdwhakd", true, 5));
    Assertions.assertEquals(123, StringUtils.parseIntegerOrElse("dahklwdhla-123dwahjdwhakd", true, 5));
    Assertions.assertEquals(123, StringUtils.parseIntegerOrElse("dahk-1lwdhla23dwahjdwhakd", true, 5));
    Assertions.assertEquals(5, StringUtils.parseIntegerOrElse("dahklwdhladwahjdwhakd", true, 5));
    Assertions.assertEquals(5, StringUtils.parseIntegerOrElse(null, true, 5));
  }

  @Test
  void parseIntegerPrefixOrElse() {
    Assertions.assertEquals(123, StringUtils.parseIntegerPrefixOrElse("+123",  5));
    Assertions.assertEquals(123, StringUtils.parseIntegerPrefixOrElse("123",  5));
    Assertions.assertEquals(123, StringUtils.parseIntegerPrefixOrElse("123dahklwdhla123dwahjdwhakd",  5));
    Assertions.assertEquals(-123, StringUtils.parseIntegerPrefixOrElse("-123dahklwdhla-123dwahjdwhakd",  5));
    Assertions.assertEquals(5, StringUtils.parseIntegerPrefixOrElse("dahklwdhladwahjdwhakd",  5));
    Assertions.assertEquals(5, StringUtils.parseIntegerPrefixOrElse(null,  5));
  }

  @Test
  void removeIntegerPrefix() {
  }

  @Test
  void parseSignAndIntegerOrElse() {
    Assertions.assertEquals(123, StringUtils.parseSignAndIntegerOrElse("+123", true, 5));
    Assertions.assertEquals(123, StringUtils.parseSignAndIntegerOrElse("123", true, 5));
    Assertions.assertEquals(123, StringUtils.parseSignAndIntegerOrElse("dahklwdhla123dwahjdwhakd", true, 5));
    Assertions.assertEquals(-123, StringUtils.parseSignAndIntegerOrElse("dahklwdhla-123dwahjdwhakd", true, 5));
    Assertions.assertEquals(-123, StringUtils.parseSignAndIntegerOrElse("dahk-1lwdhla23dwahjdwhakd", true, 5));
    Assertions.assertEquals(5, StringUtils.parseSignAndIntegerOrElse("dahklwdhladwahjdwhakd", true, 5));
    Assertions.assertEquals(5, StringUtils.parseSignAndIntegerOrElse(null, true, 5));
    Assertions.assertEquals(3, StringUtils.parseSignAndIntegerOrElse("+3", true, 5));
    Assertions.assertEquals(3, StringUtils.parseSignAndIntegerOrElse("3+", true, 5));
  }

  @Test
  void getDigits() {
    Assertions.assertEquals("123", StringUtils.getDigits("+123"));
    Assertions.assertEquals("123", StringUtils.getDigits("123"));
    Assertions.assertEquals("123", StringUtils.getDigits("dahklwdhla123dwahjdwhakd"));
    Assertions.assertEquals("123", StringUtils.getDigits("dahklwdhla-123dwahjdwhakd"));
    Assertions.assertEquals("123", StringUtils.getDigits("dahk-1lwdhla23dwahjdwhakd"));
    Assertions.assertEquals("", StringUtils.getDigits("dahklwdhladwahjdwhakd"));
    Assertions.assertEquals("", StringUtils.getDigits(null));
    Assertions.assertEquals("3", StringUtils.getDigits("+3"));
    Assertions.assertEquals("3", StringUtils.getDigits("3+"));
  }

  @Test
  void orDefault() {
    Assertions.assertEquals("default", StringUtils.orDefault("", "default"));
    Assertions.assertEquals("default", StringUtils.orDefault(" ", "default"));
    Assertions.assertEquals("default", StringUtils.orDefault("\t", "default"));
    Assertions.assertEquals("default", StringUtils.orDefault(null, "default"));
    Assertions.assertEquals("value", StringUtils.orDefault("value", "default"));
  }
}