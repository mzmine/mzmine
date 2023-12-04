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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DataPointUtilsTest {

  @Test
  void filterDataByIntensityPercent() {
    DataPoint[] dps = new DataPoint[10];
    for (int i = 0; i < dps.length; i++) {
      // sorted by intensity
      dps[i] = new SimpleDataPoint(i, 10-i);
    }
    DataPoint[] resF08 = new DataPoint[6];
    for (int i = 0; i < resF08.length; i++) {
      // sorted by intensity
      resF08[i] = new SimpleDataPoint(i, 10-i);
    }
    var f08 = DataPointUtils.filterDataByIntensityPercent(dps, 0.8, 100);
    assertArrayEquals(resF08, f08);

    var f1 = DataPointUtils.filterDataByIntensityPercent(dps, 1, 100);
    assertEquals(10, f1.length);

    var filtered = DataPointUtils.filterDataByIntensityPercent(dps, 0.95, 5);
    assertEquals(5, filtered.length);
  }

  @Test
  void removePrecursorMz() {
    DataPoint[] dps = new DataPoint[10];
    for (int i = 0; i < dps.length; i++) {
      dps[i] = new SimpleDataPoint(i, i);
    }

    Assertions.assertArrayEquals(dps, remove(dps, -2, 1));
    Assertions.assertArrayEquals(dps, remove(dps, 20, 2));
    test(dps, 0, 2);
    test(dps, 5, 2);
    test(dps, 7, 2);
    test(dps, 9, 2);
    test(dps, 10, 2);
    test(dps, 5, 20);
  }

  private static void test(final DataPoint[] dps, double center, double delta) {
    Assertions.assertArrayEquals(simpleRemove(dps, center, delta), remove(dps, center, delta));
  }

  @NotNull
  private static DataPoint[] remove(final DataPoint[] dps, double center, double delta) {
    return DataPointUtils.removePrecursorMz(dps, center, delta);
  }

  @NotNull
  private static DataPoint[] simpleRemove(final DataPoint[] dps, double center, double delta) {
    return Arrays.stream(dps).filter(dp -> !DataPointUtils.inRange(dp.getMZ(), center, delta))
        .toArray(DataPoint[]::new);
  }

}