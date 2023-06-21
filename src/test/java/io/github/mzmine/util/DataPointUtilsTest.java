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

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DataPointUtilsTest {

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