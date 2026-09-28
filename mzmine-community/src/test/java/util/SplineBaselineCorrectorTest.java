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

import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData;
import io.github.mzmine.datamodel.otherdetectors.SimpleOtherTimeSeries;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.BaselineDataBuffer;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.collections.IndexRange;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SplineBaselineCorrectorTest {

  private static final Logger logger = Logger.getLogger(
      SplineBaselineCorrectorTest.class.getName());
  @Mock
  OtherTimeSeriesData otherData;
  private BaselineDataBuffer buffer;

  @BeforeEach
  void setUp() {
    final double[] src = IntStream.range(0, 10).mapToDouble(v -> (double) v).toArray();
    buffer = createBaselineDataBuffer(src);
  }

  private BaselineDataBuffer createBaselineDataBuffer(final double[] src) {
    return createBaselineDataBuffer(src, src);
  }

  private BaselineDataBuffer createBaselineDataBuffer(final double[] rt, final double[] intensity) {
    float[] rtFloats = ArrayUtils.doubleToFloat(rt);
    var data = new SimpleOtherTimeSeries(null, rtFloats, intensity, "test", otherData);
    BaselineDataBuffer buffer = new BaselineDataBuffer();
    buffer.extractDataIntoBuffer(data);
    return buffer;
  }

  private void checkInterpolated(final BaselineDataBuffer buffer,
      final boolean expectedInterpolated, final double[] expectedX, final double[] expectedY) {
    Assertions.assertEquals(expectedInterpolated, buffer.hasInterpolatedRanges());
    // full grid is preserved -> remaining is always the full length
    Assertions.assertEquals(expectedX.length, buffer.remaining());
    Assertions.assertArrayEquals(expectedX, buffer.xBufferRemovedPeaks());
    Assertions.assertArrayEquals(expectedY, buffer.yBufferRemovedPeaks());
    // regular spacing -> only first and last point remain as landmarks
    Assertions.assertArrayEquals(new int[]{0, expectedX.length - 1},
        buffer.indicesOfInterest().toIntArray());
  }

  // --- interpolation (peak bridging) tests -------------------------------------------------------

  @Test
  public void testInterpolateRangeInMiddle() {
    // x = 0..9, y has a peak at indices 4,5,6 that should be bridged linearly back onto the ramp
    final double[] x = IntStream.range(0, 10).mapToDouble(v -> (double) v).toArray();
    final double[] y = {0, 1, 2, 3, 100, 100, 100, 7, 8, 9};
    final BaselineDataBuffer buffer = createBaselineDataBuffer(x, y);

    buffer.interpolateRanges(List.of(IndexRange.ofInclusive(4, 6)));

    checkInterpolated(buffer, true, new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
        new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
  }

  @Test
  public void testInterpolateRangeAtStart() {
    // no left anchor -> flat fill with the first retained point after the peak (y[4] = 4)
    final double[] x = IntStream.range(0, 10).mapToDouble(v -> (double) v).toArray();
    final double[] y = {100, 100, 100, 100, 4, 5, 6, 7, 8, 9};
    final BaselineDataBuffer buffer = createBaselineDataBuffer(x, y);

    buffer.interpolateRanges(List.of(IndexRange.ofInclusive(0, 3)));

    checkInterpolated(buffer, true, new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
        new double[]{4, 4, 4, 4, 4, 5, 6, 7, 8, 9});
  }

  @Test
  public void testInterpolateRangeAtEnd() {
    // no right anchor -> flat fill with the last retained point before the peak (y[6] = 6)
    final double[] x = IntStream.range(0, 10).mapToDouble(v -> (double) v).toArray();
    final double[] y = {0, 1, 2, 3, 4, 5, 6, 100, 100, 100};
    final BaselineDataBuffer buffer = createBaselineDataBuffer(x, y);

    buffer.interpolateRanges(List.of(IndexRange.ofInclusive(7, 9)));

    checkInterpolated(buffer, true, new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
        new double[]{0, 1, 2, 3, 4, 5, 6, 6, 6, 6});
  }

  @Test
  public void testInterpolateTwoRanges() {
    // peak at the start (flat fill) and a peak in the middle (linear bridge)
    final double[] x = IntStream.range(0, 10).mapToDouble(v -> (double) v).toArray();
    final double[] y = {100, 100, 2, 3, 100, 100, 100, 7, 8, 9};
    final BaselineDataBuffer buffer = createBaselineDataBuffer(x, y);

    buffer.interpolateRanges(List.of(IndexRange.ofInclusive(0, 1), IndexRange.ofInclusive(4, 6)));

    checkInterpolated(buffer, true, new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
        new double[]{2, 2, 2, 3, 4, 5, 6, 7, 8, 9});
  }

  @Test
  public void testInterpolateEmptyRanges() {
    // no ranges -> original signal is copied through untouched, flag stays false
    final double[] x = IntStream.range(0, 10).mapToDouble(v -> (double) v).toArray();
    final double[] y = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    final BaselineDataBuffer buffer = createBaselineDataBuffer(x, y);

    buffer.interpolateRanges(List.of());

    checkInterpolated(buffer, false, new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
        new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
  }

  @Test
  public void testInterpolateDoesNotMutateMainBuffers() {
    // the bridged data must land in the removed-peaks buffers only, the original signal stays intact
    final double[] x = IntStream.range(0, 10).mapToDouble(v -> (double) v).toArray();
    final double[] y = {0, 1, 2, 3, 100, 100, 100, 7, 8, 9};
    final BaselineDataBuffer buffer = createBaselineDataBuffer(x, y);

    buffer.interpolateRanges(List.of(IndexRange.ofInclusive(4, 6)));

    Assertions.assertNotSame(buffer.yBuffer(), buffer.yBufferRemovedPeaks());
    Assertions.assertArrayEquals(y, Arrays.copyOf(buffer.yBuffer(), 10));
  }

  /**
   * Regression test: when a single buffer (i.e. a reused baseline corrector) processes a trace with
   * no ranges and then another trace with ranges, the "removed peaks" buffers must not alias the
   * main buffers - otherwise bridging the second trace would overwrite the original signal that
   * {@code subSampleAndCorrect} still needs as the data to correct. This only surfaces in the task
   * (one corrector reused across multiple traces with peak exclusion enabled), never in the preview
   * (fresh corrector per trace).
   */
  @Test
  public void testReusedBufferAfterEmptyRangesDoesNotCorruptMainBuffers() {
    // trace A: no ranges -> must not alias the removed-peaks buffers to the main buffers
    buffer.interpolateRanges(List.of());

    // trace B: re-extract into the SAME buffer. Same length -> capacity is not reallocated, so any
    // alias established by trace A would still be in place here.
    final double[] srcB = IntStream.range(0, 10).mapToDouble(v -> (double) v).toArray();
    final var dataB = new SimpleOtherTimeSeries(null, ArrayUtils.doubleToFloat(srcB), srcB, "testB",
        otherData);
    buffer.extractDataIntoBuffer(dataB);

    // snapshot the original (main) buffers before bridging ranges
    final double[] xMainBefore = Arrays.copyOf(buffer.xBuffer(), buffer.xBuffer().length);
    final double[] yMainBefore = Arrays.copyOf(buffer.yBuffer(), buffer.yBuffer().length);

    // trace B: bridge a middle range
    buffer.interpolateRanges(List.of(IndexRange.ofInclusive(4, 6)));

    // the removed-peaks buffers must be distinct instances from the main buffers ...
    Assertions.assertNotSame(buffer.xBuffer(), buffer.xBufferRemovedPeaks());
    Assertions.assertNotSame(buffer.yBuffer(), buffer.yBufferRemovedPeaks());

    // ... so bridging ranges must not have mutated the original signal in the main buffers
    Assertions.assertArrayEquals(xMainBefore, buffer.xBuffer());
    Assertions.assertArrayEquals(yMainBefore, buffer.yBuffer());

    // and the bridged data ends up in the removed-peaks buffer on the full grid (y[4..6] bridged
    // linearly between y[3]=3 and y[7]=7 -> the plain 0..9 ramp)
    checkInterpolated(buffer, true, new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
        new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
  }
}
