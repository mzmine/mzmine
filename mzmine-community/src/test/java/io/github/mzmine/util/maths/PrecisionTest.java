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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util.maths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;

class PrecisionTest {

  @Test
  void testEquals() {
    // Positive test case
    assertTrue(Precision.equals(100.0, 100.5, 0.5));
    // Negative test case
    assertFalse(Precision.equals(100.0, 101.0, 0.5));
    // Test with very small delta
    assertTrue(Precision.equals(1.0000001, 1.0000002, 0.0000001));
    assertFalse(Precision.equals(1.0000001, 1.0000003, 0.0000001));
    // Test with very large numbers
    assertTrue(Precision.equals(1e10, 1e10 + 1e5, 1e6));
    assertFalse(Precision.equals(1e10, 1e10 + 1e7, 1e6));
    // Test with NaN
    assertFalse(Precision.equals(Double.NaN, 1.0, 1.0));
    assertFalse(Precision.equals(1.0, Double.NaN, 1.0));
    // Test with infinities
    assertFalse(Precision.equals(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1.0));
    assertTrue(Precision.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
    assertTrue(Precision.equals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, 1.0));
  }

  @Test
  void testEquals1() {
    // Positive test case for float
    assertTrue(Precision.equals(100.0f, 100.5f, 0.5f));
    // Negative test case for float
    assertFalse(Precision.equals(100.0f, 101.0f, 0.5f));
    // Test with very small delta
    assertTrue(Precision.equals(1.00001f, 1.00002f, 0.0001f));
    assertFalse(Precision.equals(1.0001f, 1.0003f, 0.0001f));
    // Test with very large numbers for float
    assertTrue(Precision.equals(1e10f, 1e10f + 1e5f, 1e6f));
    assertFalse(Precision.equals(1e10f, 1e10f + 1e7f, 1e6f));
    // Test with NaN
    assertFalse(Precision.equals(Float.NaN, 1.0f, 1.0f));
    assertFalse(Precision.equals(1.0f, Float.NaN, 1.0f));
    // Test with infinities
    assertFalse(Precision.equals(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 1.0f));
    assertTrue(Precision.equals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 1.0f));
    assertTrue(Precision.equals(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, 1.0f));
  }

  @Test
  public void testFloatSignificance() {
    assertTrue(Precision.equalSignificance(2.223635d, 2.2236354d, 5));
    assertTrue(Precision.equalFloatSignificance(2.223635f, 2.2236354f));
    assertTrue(Precision.equalFloatSignificance(3.29015f, 3.2901502f));
  }

  @Test
  void testEquals2() {
    // absolute delta dominates
    assertTrue(Precision.equals(10.0, 10.4, 0.5, 0.01));
    assertFalse(Precision.equals(10.0, 10.6, 0.5, 0.01));

    // relative delta dominates for large numbers
    assertTrue(Precision.equals(1_000_000.0, 1_001_000.0, 10.0, 0.001));
    assertFalse(Precision.equals(1_000_000.0, 1_100_000.0, 10.0, 0.001));

    // NaN handling (should be false)
    assertFalse(Precision.equals(Double.NaN, 1.0, 0.1, 0.1));
    assertFalse(Precision.equals(1.0, Double.NaN, 0.1, 0.1));
  }

  @Test
  void testEquals3() {
    // absolute delta dominates (float)
    assertTrue(Precision.equals(10.0f, 10.4f, 0.5f, 0.01f));
    assertFalse(Precision.equals(10.0f, 10.6f, 0.5f, 0.01f));

    // relative delta dominates (float)
    assertTrue(Precision.equals(100_000f, 100_500f, 10f, 0.01f));
    assertFalse(Precision.equals(100_000f, 120_000f, 10f, 0.01f));

    // NaN handling (should be false)
    assertFalse(Precision.equals(Float.NaN, 1.0f, 0.1f, 0.1f));
    assertFalse(Precision.equals(1.0f, Float.NaN, 0.1f, 0.1f));
  }

  @Test
  void testEquals4() {
    // Symmetry and zero checks for double 4-arg equals
    assertTrue(Precision.equals(0.0, 0.0, 0.0, 0.0));
    assertTrue(Precision.equals(-100.0, -100.0001, 0.001, 1e-6));
    assertTrue(Precision.equals(-100.0001, -100.0, 0.001, 1e-6));
    assertFalse(Precision.equals(-100.0, -100.1, 0.001, 1e-6));
  }

  @Test
  void testEquals5() {
    // Symmetry and zero checks for float 4-arg equals
    assertTrue(Precision.equals(0.0f, 0.0f, 0.0f, 0.0f));
    assertTrue(Precision.equals(-100.0f, -100.0001f, 0.001f, 1e-6f));
    assertTrue(Precision.equals(-100.0001f, -100.0f, 0.001f, 1e-6f));
    assertFalse(Precision.equals(-100.0f, -100.1f, 0.001f, 1e-6f));
  }

  @Test
  void round() {
    // HALF_UP by default
    BigDecimal rounded = Precision.round(1234.5678, 3);
    assertEquals("1.23E+3", rounded.toEngineeringString());

    // check negative number
    BigDecimal roundedNeg = Precision.round(-0.00495, 2);
    // -0.00495 with 2 significant digits HALF_UP -> about -0.005
    assertEquals("-0.0050", roundedNeg.toPlainString());
  }

  @Test
  void testRound() {
    // Explicit rounding mode HALF_DOWN
    BigDecimal halfDown = Precision.round(1.245, 3, RoundingMode.HALF_DOWN);
    assertEquals("1.24", halfDown.toPlainString());

    // Explicit rounding mode DOWN
    BigDecimal down = Precision.round(9.999, 2, RoundingMode.DOWN);
    assertEquals("9.9", down.toPlainString());
  }

  @Test
  void testToString() {
    // Uses HALF_UP
    assertEquals("1230", Precision.toString(1234.5, 3));
    assertEquals("0.00123", Precision.toString(0.0012345, 3));
  }

  @Test
  void testToString1() {
    // Explicit rounding mode
    String halfDown = Precision.toString(1.245, 3, RoundingMode.HALF_DOWN);
    assertEquals("1.24", halfDown);

    String down = Precision.toString(9.999, 2, RoundingMode.DOWN);
    assertEquals("9.9", down);
  }

  @Test
  void testToString2() {
    // maxLength large enough so no scientific notation
    String s1 = Precision.toString(1234567.0, 3, 10);
    assertEquals("1230000", s1);

    String s2 = Precision.toString(0.000123456, 3, 10);
    assertEquals("0.000123", s2);
  }

  @Test
  void testToString3() {
    // Force scientific notation when digits exceed maxLength
    String s = Precision.toString(1234567.0, 3, RoundingMode.HALF_UP, 3);
    assertTrue(s.contains("E"));
  }

  @Test
  void equalDoubleSignificance() {
    // equal within 14 significant digits
    double a = 1.23456789012345;
    double b = 1.23456789012346;
    assertTrue(Precision.equalDoubleSignificance(a, b));

    // differ at higher significance
    double c = 1.23456789012345;
    double d = 1.23456789022345;
    assertFalse(Precision.equalDoubleSignificance(c, d));

    // NaN handling
    assertTrue(Precision.equalSignificance(Double.NaN, Double.NaN, 14));
    assertFalse(Precision.equalSignificance(Double.NaN, 1.0, 14));
  }

  @Test
  void equalSignificance() {
    // basic equality and rounding behavior for double
    assertTrue(Precision.equalSignificance(1.234567, 1.2345671, 6));
    assertFalse(Precision.equalSignificance(1.234567, 1.234678, 6));

    // exact equality
    assertTrue(Precision.equalSignificance(10.0, 10.0, 5));

    // large magnitude numbers
    assertTrue(Precision.equalSignificance(1.0e10, 1.000000000001e10, 10));
    assertTrue(Precision.equalSignificance(5.293524e10, 5.293524000001e10, 10));
    assertTrue(Precision.equalSignificance(1.0e10, 1.000000000001e10, 10));
    assertFalse(Precision.equalSignificance(1.0e10, 1.0001e10, 10));
  }

  @Test
  void equalFloatSignificance() {
    // primitive float variant
    assertTrue(Precision.equalFloatSignificance(1.234567f, 1.234568f));
    assertFalse(Precision.equalFloatSignificance(1.234567f, 1.234677f));

    // NaN handling
    assertTrue(Precision.equalSignificance(Float.NaN, Float.NaN, 6));
    assertFalse(Precision.equalSignificance(Float.NaN, 1.0f, 6));
  }

  @Test
  void testEqualFloatSignificance() {
    // boxed Float variant with null handling
    Float a = 1.234567f;
    Float b = 1.234568f;
    assertTrue(Precision.equalFloatSignificance(a, b));

    Float c = null;
    Float d = null;
    assertTrue(Precision.equalFloatSignificance(c, d));

    Float e = null;
    Float f = 1.0f;
    assertFalse(Precision.equalFloatSignificance(e, f));
  }

  @Test
  void testEqualSignificance() {
    // float with explicit significant digits
    assertTrue(Precision.equalSignificance(1.234567f, 1.234568f, 6));
    assertFalse(Precision.equalSignificance(1.234567f, 1.234677f, 6));

    // exact equality
    assertTrue(Precision.equalSignificance(10.0f, 10.0f, 3));
  }
}
