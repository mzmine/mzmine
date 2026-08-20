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
    float[] floats = ArrayUtils.doubleToFloat(src);
    var data = new SimpleOtherTimeSeries(null, floats, src, "test", otherData);
    BaselineDataBuffer buffer = new BaselineDataBuffer();
    buffer.extractDataIntoBuffer(data);
    return buffer;
  }

  private void check(final BaselineDataBuffer buffer, final int expectedRemaining,
      final double[] expectedX, final int[] expectedIndicesOfInterest) {
    Assertions.assertEquals(expectedRemaining, buffer.remaining());
    Assertions.assertArrayEquals(expectedX, buffer.xBufferRemovedPeaks());

    // indices of interes
    Assertions.assertArrayEquals(expectedIndicesOfInterest,
        buffer.indicesOfInterest().toIntArray());
    // for debugging purposes
//    logger.info(
//        "Indices of interest: " + buffer.indicesOfInterest().intStream().mapToObj(String::valueOf)
//            .collect(Collectors.joining(", ")));
//    logger.info(Arrays.stream(buffer.xBufferRemovedPeaks()).mapToObj(String::valueOf)
//        .collect(Collectors.joining(", ")));
  }


  @Test
  public void testRangeAtStart() {
    IndexRange r = IndexRange.ofInclusive(0, 3);
    buffer.removeRangesFromArray(List.of(r));

    check(buffer, 7, new double[]{0, 4, 5, 6, 7, 8, 9, 0, 0, 0}, new int[]{0, 1, 6});
  }

  @Test
  public void testRangeAtEnd() {
    IndexRange r = IndexRange.ofInclusive(7, 9);
    buffer.removeRangesFromArray(List.of(r));

    check(buffer, 8, new double[]{0, 1, 2, 3, 4, 5, 6, 9, 0, 0}, new int[]{0, 6, 7});
  }

  @Test
  public void testRangeInMiddle() {
    IndexRange r = IndexRange.ofInclusive(4, 6);
    buffer.removeRangesFromArray(List.of(r));

    check(buffer, 7, new double[]{0, 1, 2, 3, 7, 8, 9, 0, 0, 0}, new int[]{0, 3, 4, 6});
  }

  @Test
  public void testTwoRangesStart() {
    IndexRange r1 = IndexRange.ofInclusive(0, 1);
    IndexRange r2 = IndexRange.ofInclusive(4, 6);

    buffer.removeRangesFromArray(List.of(r1, r2));

    check(buffer, 6, new double[]{0, 2, 3, 7, 8, 9, 0, 0, 0, 0}, new int[]{0, 1, 2, 3, 5});
  }

  @Test
  public void testTwoRangesEnd() {
    IndexRange r1 = IndexRange.ofInclusive(4, 6);
    IndexRange r2 = IndexRange.ofInclusive(8, 9);

    buffer.removeRangesFromArray(List.of(r1, r2));

    check(buffer, 6, new double[]{0, 1, 2, 3, 7, 9, 0, 0, 0, 0}, new int[]{0, 3, 4, 5});
  }

  @Test
  public void testEmptyRanges() {
    buffer.removeRangesFromArray(List.of());

    check(buffer, 10, new double[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, new int[]{0, 9});
  }

  /**
   * Regression test: when a single buffer (i.e. a reused baseline corrector) processes a trace with
   * no removed ranges and then another trace with removed ranges, the "removed peaks" buffers must
   * not alias the main buffers. Previously the empty-ranges branch did
   * {@code xBufferRemovedPeaks = xBuffer}, so the subsequent compaction wrote into the main buffers,
   * destroying the original signal that {@code subSampleAndCorrect} still needs as the data to
   * correct. This only surfaced in the task (one corrector reused across multiple traces with peak
   * exclusion enabled), never in the preview (fresh corrector per trace).
   */
  @Test
  public void testReusedBufferAfterEmptyRangesDoesNotCorruptMainBuffers() {
    // trace A: no ranges removed -> must not alias the removed-peaks buffers to the main buffers
    buffer.removeRangesFromArray(List.of());

    // trace B: re-extract into the SAME buffer. Same length -> capacity is not reallocated, so any
    // alias established by trace A would still be in place here.
    final double[] srcB = IntStream.range(0, 10).mapToDouble(v -> (double) v).toArray();
    final var dataB = new SimpleOtherTimeSeries(null, ArrayUtils.doubleToFloat(srcB), srcB, "testB",
        otherData);
    buffer.extractDataIntoBuffer(dataB);

    // snapshot the original (main) buffers before removing ranges
    final double[] xMainBefore = Arrays.copyOf(buffer.xBuffer(), buffer.xBuffer().length);
    final double[] yMainBefore = Arrays.copyOf(buffer.yBuffer(), buffer.yBuffer().length);

    // trace B: remove a middle range
    buffer.removeRangesFromArray(List.of(IndexRange.ofInclusive(4, 6)));

    // the removed-peaks buffers must be distinct instances from the main buffers ...
    Assertions.assertNotSame(buffer.xBuffer(), buffer.xBufferRemovedPeaks());
    Assertions.assertNotSame(buffer.yBuffer(), buffer.yBufferRemovedPeaks());

    // ... so removing ranges must not have mutated the original signal in the main buffers
    Assertions.assertArrayEquals(xMainBefore, buffer.xBuffer());
    Assertions.assertArrayEquals(yMainBefore, buffer.yBuffer());

    // and the compacted data ends up in the removed-peaks buffer as usual
    check(buffer, 7, new double[]{0, 1, 2, 3, 7, 8, 9, 0, 0, 0}, new int[]{0, 3, 4, 6});
  }
}
