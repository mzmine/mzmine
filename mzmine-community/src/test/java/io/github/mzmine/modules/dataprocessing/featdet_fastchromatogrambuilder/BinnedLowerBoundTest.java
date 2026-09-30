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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BinnedLowerBoundTest {

  @Test
  void equalsBinarySearch() {
    final Random random = new Random(3);
    for (final double binWidth : new double[]{0.0001, 0.005, 1, 1000}) {
      final double[] values = new double[5000];
      for (int i = 0; i < values.length; i++) {
        // clusters and duplicates
        values[i] = 100 + random.nextInt(200) * 4.5 + random.nextDouble() * 0.01;
      }
      values[10] = values[11];
      Arrays.sort(values);
      final BinnedLowerBound lookup = new BinnedLowerBound(values, binWidth);
      for (int k = 0; k < 20000; k++) {
        final double key = 90 + random.nextDouble() * 1000;
        Assertions.assertEquals(ChannelConsolidation.lowerBound(values, key),
            lookup.lowerBound(key), "key " + key);
        Assertions.assertEquals(ChannelConsolidation.upperBound(values, key),
            lookup.upperBound(key), "key " + key);
      }
      for (final double value : values) {
        Assertions.assertEquals(ChannelConsolidation.lowerBound(values, value),
            lookup.lowerBound(value));
        Assertions.assertEquals(ChannelConsolidation.upperBound(values, value),
            lookup.upperBound(value));
        final double below = Math.nextDown(value);
        Assertions.assertEquals(ChannelConsolidation.lowerBound(values, below),
            lookup.lowerBound(below));
        Assertions.assertEquals(ChannelConsolidation.upperBound(values, below),
            lookup.upperBound(below));
        final double above = Math.nextUp(value);
        Assertions.assertEquals(ChannelConsolidation.lowerBound(values, above),
            lookup.lowerBound(above));
        Assertions.assertEquals(ChannelConsolidation.upperBound(values, above),
            lookup.upperBound(above));
      }
    }
  }

  @Test
  void emptyAndSingleValue() {
    Assertions.assertEquals(0, new BinnedLowerBound(new double[0], 0.01).lowerBound(5));
    final BinnedLowerBound single = new BinnedLowerBound(new double[]{5}, 0.01);
    Assertions.assertEquals(0, single.lowerBound(4));
    Assertions.assertEquals(0, single.lowerBound(5));
    Assertions.assertEquals(1, single.lowerBound(6));
    Assertions.assertEquals(0, single.upperBound(4));
    Assertions.assertEquals(1, single.upperBound(5));
    Assertions.assertEquals(1, single.upperBound(6));
  }
}
