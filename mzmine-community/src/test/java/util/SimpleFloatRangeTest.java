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

package util;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.SimpleRange.SimpleFloatRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link SimpleFloatRange} class. This class validates the functionality of the
 * {@code isConnected} method, which determines whether two ranges are connected (overlap or
 * touch).
 */
public class SimpleFloatRangeTest {

  /**
   * Test case where the ranges overlap.
   */
  @Test
  public void testIsConnectedOverlap() {
    final SimpleFloatRange range1 = new SimpleFloatRange(1f, 5f);
    final SimpleFloatRange range2 = new SimpleFloatRange(4f, 8f);

    Assertions.assertTrue(range1.isConnected(range2));
    Assertions.assertTrue(range2.isConnected(range1));
  }

  /**
   * Test case where the ranges touch at boundaries.
   */
  @Test
  public void testIsConnectedTouchAtBoundary() {
    final SimpleFloatRange range1 = new SimpleFloatRange(1f, 5f);
    final SimpleFloatRange range2 = new SimpleFloatRange(5f, 8f);

    Assertions.assertTrue(range1.isConnected(range2));
    Assertions.assertTrue(range2.isConnected(range1));
  }

  /**
   * Test case where one range completely encloses the other.
   */
  @Test
  public void testIsConnectedEnclosedRange() {
    final SimpleFloatRange range1 = new SimpleFloatRange(1f, 10f);
    final SimpleFloatRange range2 = new SimpleFloatRange(3f, 7f);

    Assertions.assertTrue(range1.isConnected(range2));
    Assertions.assertTrue(range2.isConnected(range1));
  }

  /**
   * Test case where the ranges do not connect.
   */
  @Test
  public void testIsConnectedNoConnection() {
    final SimpleFloatRange range1 = new SimpleFloatRange(1f, 5f);
    final SimpleFloatRange range2 = new SimpleFloatRange(6f, 10f);

    Assertions.assertFalse(range1.isConnected(range2));
    Assertions.assertFalse(range2.isConnected(range1));
  }

  /**
   * Test case where both ranges are identical.
   */
  @Test
  public void testIsConnectedIdenticalRanges() {
    final SimpleFloatRange range1 = new SimpleFloatRange(1f, 5f);
    final SimpleFloatRange range2 = new SimpleFloatRange(1f, 5f);

    Assertions.assertTrue(range1.isConnected(range2));
    Assertions.assertTrue(range2.isConnected(range1));
  }

  /**
   * Test case with a connection to a Guava Range with overlapping boundaries.
   */
  @Test
  public void testIsConnectedGuavaOverlap() {
    final SimpleFloatRange range1 = new SimpleFloatRange(1f, 5f);
    final Range<Float> guavaRange = Range.closed(4f, 8f);

    Assertions.assertTrue(range1.isConnected(guavaRange));
  }

  /**
   * Test case with a connection to a Guava Range that touches at the boundary.
   */
  @Test
  public void testIsConnectedGuavaTouchAtBoundary() {
    final SimpleFloatRange range1 = new SimpleFloatRange(1f, 5f);
    final Range<Float> guavaRange = Range.closed(5f, 10f);

    Assertions.assertTrue(range1.isConnected(guavaRange));
  }

  /**
   * Test case with no connection to a Guava Range.
   */
  @Test
  public void testIsConnectedGuavaNoConnection() {
    final SimpleFloatRange range1 = new SimpleFloatRange(1f, 5f);
    final Range<Float> guavaRange = Range.closed(6f, 10f);

    Assertions.assertFalse(range1.isConnected(guavaRange));
  }

  /**
   * The float representation only keeps ~7 significant digits, so bounds that differ in the double
   * world can collapse onto the same float bound.
   */
  @Test
  public void testFloatPrecisionCollapsesNeighbouringBounds() {
    // the ulp at 100 is about 7.6E-6, so both bounds round to the exact same float
    final SimpleFloatRange range = new SimpleFloatRange(100.000001f, 100.000002f);

    Assertions.assertEquals(100f, range.lowerBound());
    Assertions.assertEquals(100f, range.upperBound());
    // the range degenerates to a single value, but still contains everything that rounds to it
    Assertions.assertEquals(0f, range.length());
    Assertions.assertTrue(range.contains(100.0000015f));
  }
}
