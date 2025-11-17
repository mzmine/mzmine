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

    // todo: fix:
//    assertTrue(Precision.equalFloatSignificance(3.29015f, 3.2901502f));
  }
}
