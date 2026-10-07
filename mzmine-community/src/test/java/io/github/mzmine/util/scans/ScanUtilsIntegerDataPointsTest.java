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

package io.github.mzmine.util.scans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.util.scans.ScanUtils.IntegerMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ScanUtils#integerDataPoints(DataPoint[], IntegerMode)}. The MAX and SUM cases used to
 * be swapped and the output order was that of a HashMap.
 */
class ScanUtilsIntegerDataPointsTest {

  /**
   * 100.1 and 99.8 both round to 100; 55.4 and 55.2 both round to 55. Deliberately unsorted so the
   * ordering of the result is actually exercised.
   */
  private static DataPoint[] input() {
    return new DataPoint[]{new SimpleDataPoint(100.1, 30.0), new SimpleDataPoint(55.4, 5.0),
        new SimpleDataPoint(99.8, 70.0), new SimpleDataPoint(200.2, 10.0),
        new SimpleDataPoint(55.2, 15.0)};
  }

  @Test
  @DisplayName("MAX keeps the highest intensity of the merged signals")
  void maxTakesMaximum() {

    final DataPoint[] result = ScanUtils.integerDataPoints(input(), IntegerMode.MAX);

    assertEquals(3, result.length);
    assertEquals(55.0, result[0].getMZ());
    assertEquals(15.0, result[0].getIntensity());
    assertEquals(100.0, result[1].getMZ());
    assertEquals(70.0, result[1].getIntensity());
    assertEquals(200.0, result[2].getMZ());
    assertEquals(10.0, result[2].getIntensity());
  }

  @Test
  @DisplayName("SUM adds the intensities of the merged signals")
  void sumAddsIntensities() {

    final DataPoint[] result = ScanUtils.integerDataPoints(input(), IntegerMode.SUM);

    assertEquals(3, result.length);
    assertEquals(55.0, result[0].getMZ());
    assertEquals(20.0, result[0].getIntensity());
    assertEquals(100.0, result[1].getMZ());
    assertEquals(100.0, result[1].getIntensity());
    assertEquals(200.0, result[2].getMZ());
    assertEquals(10.0, result[2].getIntensity());
  }

  @Test
  @DisplayName("Output is sorted by ascending m/z")
  void resultIsSortedByMz() {

    for (final IntegerMode mode : IntegerMode.values()) {
      final DataPoint[] result = ScanUtils.integerDataPoints(input(), mode);

      for (int i = 1; i < result.length; i++) {
        assertTrue(result[i - 1].getMZ() < result[i].getMZ(),
            () -> mode + " produced an unsorted peak list");
      }
    }
  }

  @Test
  @DisplayName("An empty spectrum stays empty")
  void emptyInput() {
    assertEquals(0, ScanUtils.integerDataPoints(new DataPoint[0], IntegerMode.SUM).length);
  }
}
