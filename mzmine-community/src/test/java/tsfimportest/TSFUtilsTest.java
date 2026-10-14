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

package tsfimportest;

import io.github.mzmine.modules.io.import_rawdata_bruker_tsf.TSFUtils;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TSFUtils#filterZeroIntensities} - the pure zero-run deletion used when
 * loading Bruker TSF profile spectra. Kept native-free (heap segments only) so it runs on every
 * platform.
 */
class TSFUtilsTest {

  /**
   * Runs the filter on heap segments wrapping the given arrays.
   */
  private static int filter(final int @NotNull [] intensities, final int numDataPoints,
      final double scalingFactor, final double @NotNull [] outIndices,
      final double @NotNull [] outIntensities) {
    return TSFUtils.filterZeroIntensities(MemorySegment.ofArray(intensities), numDataPoints,
        scalingFactor, MemorySegment.ofArray(outIndices), outIntensities);
  }

  @Test
  void emptySpectrumReturnsZero() {
    final double[] outIndices = new double[4];
    final double[] outIntensities = new double[4];

    final int numValues = filter(new int[4], 0, 1d, outIndices, outIntensities);

    Assertions.assertEquals(0, numValues);
  }

  @Test
  void allZeroSpectrumKeepsNothing() {
    final double[] outIndices = new double[4];
    final double[] outIntensities = new double[4];

    final int numValues = filter(new int[]{0, 0, 0, 0}, 4, 1d, outIndices, outIntensities);

    // nothing kept -> not even a trailing conversion boundary is appended
    Assertions.assertEquals(0, numValues);
  }

  @Test
  void peakWithShouldersIsKept() {
    final double[] outIndices = new double[5];
    final double[] outIntensities = new double[5];

    final int numValues = filter(new int[]{0, 10, 0, 20, 5}, 5, 1d, outIndices, outIntensities);

    // kept indices 1..4 plus a trailing boundary that repeats the last index with intensity 0
    Assertions.assertEquals(5, numValues);
    Assertions.assertArrayEquals(new double[]{1, 2, 3, 4, 4},
        Arrays.copyOf(outIndices, numValues));
    Assertions.assertArrayEquals(new double[]{10, 0, 20, 5, 0},
        Arrays.copyOf(outIntensities, numValues));
  }

  /**
   * Regression test for the off-by-one that dropped the last real data point: a non-zero value in
   * the very last bin must survive filtering.
   */
  @Test
  void lastDataPointIsNotDropped() {
    final double[] outIndices = new double[4];
    final double[] outIntensities = new double[4];

    final int numValues = filter(new int[]{0, 0, 0, 7}, 4, 1d, outIndices, outIntensities);

    // index 2 kept as the peak's left shoulder, index 3 (the last bin) kept with its real
    // intensity, then the trailing zero boundary
    Assertions.assertEquals(3, numValues);
    Assertions.assertArrayEquals(new double[]{2, 3, 3}, Arrays.copyOf(outIndices, numValues));
    Assertions.assertArrayEquals(new double[]{0, 7, 0}, Arrays.copyOf(outIntensities, numValues));
  }

  @Test
  void isolatedPeakKeepsBothShoulders() {
    final double[] outIndices = new double[5];
    final double[] outIntensities = new double[5];

    final int numValues = filter(new int[]{0, 0, 5, 0, 0}, 5, 1d, outIndices, outIntensities);

    Assertions.assertEquals(4, numValues);
    Assertions.assertArrayEquals(new double[]{1, 2, 3, 4}, Arrays.copyOf(outIndices, numValues));
    Assertions.assertArrayEquals(new double[]{0, 5, 0, 0},
        Arrays.copyOf(outIntensities, numValues));
  }

  /**
   * Only the first {@code numDataPoints} entries may be read - stale data left over in a reused
   * buffer beyond that boundary must be ignored.
   */
  @Test
  void staleDataBeyondNumDataPointsIsIgnored() {
    final double[] outIndices = new double[5];
    final double[] outIntensities = new double[5];
    // indices 3 and 4 hold stale non-zero values from a hypothetical previous, larger scan
    final int[] intensities = new int[]{0, 10, 0, 999, 999};

    final int numValues = filter(intensities, 3, 1d, outIndices, outIntensities);

    // index 1 (=10) and index 2 (kept because its predecessor is != 0), then the boundary at the
    // last valid index (2). The stale 999s are never read.
    Assertions.assertEquals(3, numValues);
    Assertions.assertArrayEquals(new double[]{1, 2, 2}, Arrays.copyOf(outIndices, numValues));
    Assertions.assertArrayEquals(new double[]{10, 0, 0}, Arrays.copyOf(outIntensities, numValues));
  }

  @Test
  void scalingFactorIsApplied() {
    final double[] outIndices = new double[4];
    final double[] outIntensities = new double[4];

    final int numValues = filter(new int[]{0, 10, 20, 0}, 4, 2.5d, outIndices, outIntensities);

    Assertions.assertEquals(4, numValues);
    Assertions.assertArrayEquals(new double[]{25, 50, 0, 0},
        Arrays.copyOf(outIntensities, numValues));
  }

  /**
   * Intensities are uint32. Values >= 2^31 are negative as a Java int and must neither be treated
   * as zero (for the neighbour check) nor be converted as negative values.
   */
  @Test
  void unsignedIntensitiesAboveIntMaxAreKept() {
    final double[] outIndices = new double[5];
    final double[] outIntensities = new double[5];
    // -1 is 0xFFFFFFFF = 4294967295 as uint32
    final int[] intensities = new int[]{0, 0, -1, 0, 0};

    final int numValues = filter(intensities, 5, 1d, outIndices, outIntensities);

    // both shoulders of the peak at index 2 are kept
    Assertions.assertEquals(4, numValues);
    Assertions.assertArrayEquals(new double[]{1, 2, 3, 4}, Arrays.copyOf(outIndices, numValues));
    Assertions.assertArrayEquals(new double[]{0, 4294967295d, 0, 0},
        Arrays.copyOf(outIntensities, numValues));
  }
}
