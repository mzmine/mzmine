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

package io.github.mzmine.modules.dataprocessing.featdet_smoothing.savitzkygolay;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SavitzkyGolayFilterTest {

  @Test
  void windowedConvolveMatchesStandaloneConvolve() {
    final double[] weights = SavitzkyGolayFilter.getNormalizedWeights(5);

    final double[] window = new double[]{1, 4, 9, 16, 9, 4, 1};
    final double[] expected = SavitzkyGolayFilter.convolve(window, weights);

    // embed the same window into a larger array surrounded by very different values
    final double[] full = new double[]{1000, 1000, 1, 4, 9, 16, 9, 4, 1, 1000, 1000};
    final int from = 2;
    final int to = from + window.length;
    final double[] out = new double[full.length];

    SavitzkyGolayFilter.convolve(full, from, to, weights, out);

    // results inside the window must equal the standalone convolution (no bleeding from neighbours)
    for (int i = 0; i < window.length; i++) {
      assertEquals(expected[i], out[from + i], 1e-12, "mismatch at window index " + i);
    }
  }

  @Test
  void windowedConvolveLeavesOutsideUntouched() {
    final double[] weights = SavitzkyGolayFilter.getNormalizedWeights(5);
    final double[] full = new double[]{1, 2, 3, 4, 5, 6, 7, 8};
    final double[] out = new double[full.length];
    Arrays.fill(out, -1.0);

    final int from = 3;
    final int to = 6;
    SavitzkyGolayFilter.convolve(full, from, to, weights, out);

    // positions outside [from, to) must remain at their sentinel value
    for (int i = 0; i < full.length; i++) {
      if (i < from || i >= to) {
        assertEquals(-1.0, out[i], "position " + i + " outside the window was modified");
      }
    }
  }

  @Test
  void neighbouringWindowsDoNotBleed() {
    final double[] weights = SavitzkyGolayFilter.getNormalizedWeights(5);
    // two ranges that would heavily influence each other if convolved as one array
    final double[] full = new double[]{10, 10, 10, 0, 0, 1000, 1000, 1000};
    final double[] out = new double[full.length];

    SavitzkyGolayFilter.convolve(full, 0, 3, weights, out);
    SavitzkyGolayFilter.convolve(full, 5, 8, weights, out);

    // first range smoothed in isolation -> stays around 10, not pulled up by the 1000s
    final double[] firstExpected = SavitzkyGolayFilter.convolve(new double[]{10, 10, 10}, weights);
    assertArrayEquals(firstExpected, new double[]{out[0], out[1], out[2]}, 1e-12);

    final double[] secondExpected = SavitzkyGolayFilter.convolve(new double[]{1000, 1000, 1000},
        weights);
    assertArrayEquals(secondExpected, new double[]{out[5], out[6], out[7]}, 1e-12);
  }
}
