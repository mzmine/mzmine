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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UnusedDataPointsTest {

  @Test
  void sortMergesSortedRunsStably() {
    final Random random = new Random(5);
    final UnusedDataPoints unused = new UnusedDataPoints();
    for (int round = 0; round < 200; round++) {
      unused.reset(round);
      // up to 5 sorted runs with duplicate m/z values, the intensity marks the insertion order
      final List<double[]> expected = new ArrayList<>();
      final int runs = 1 + random.nextInt(5);
      int order = 0;
      for (int r = 0; r < runs; r++) {
        double mz = 100 + random.nextInt(20);
        final int length = random.nextInt(300);
        for (int i = 0; i < length; i++) {
          mz += random.nextInt(3) == 0 ? 0 : random.nextDouble();
          unused.add(mz, order);
          expected.add(new double[]{mz, order});
          order++;
        }
      }
      expected.sort(Comparator.comparingDouble((double[] p) -> p[0]));
      unused.sort();
      for (int i = 0; i < expected.size(); i++) {
        final double[] point = expected.get(i);
        // the closest data point to its own m/z with its own intensity is itself
        final int found = unused.findClosest(point[0], 0, point[1], point[1]);
        Assertions.assertTrue(found >= 0, "data point %d missing".formatted(i));
        Assertions.assertEquals(point[0], unused.mz(found));
      }
      // sorted and stable: the order of equal m/z values keeps the insertion order
      for (int i = 0; i < expected.size(); i++) {
        Assertions.assertEquals(expected.get(i)[0], unused.mz(i), "round " + round);
        Assertions.assertEquals(expected.get(i)[1], unused.intensity(i), "round " + round);
      }
    }
  }
}
