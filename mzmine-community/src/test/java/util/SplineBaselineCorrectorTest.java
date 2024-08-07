/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.AbstractBaselineCorrector;
import io.github.mzmine.util.collections.SimpleIndexRange;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SplineBaselineCorrectorTest {

  @Test
  public void testRangeAtStart() {
    SimpleIndexRange r = new SimpleIndexRange(0, 3);

    final double[] src = new double[]{3, 1, 1, 2, 2, 2, 2, 2, 2, 2};
    final double[] dst = new double[10];

    final int remaining = AbstractBaselineCorrector.removeRangesFromArray(List.of(r), 10, src,
        dst);

    Assertions.assertEquals(7, remaining);
    Assertions.assertArrayEquals(new double[]{3, 2, 2, 2, 2, 2, 2, 0, 0, 0}, dst);
  }

  @Test
  public void testRangeAtEnd() {
    SimpleIndexRange r = new SimpleIndexRange(7, 9);

    final double[] src = new double[]{2, 2, 2, 2, 2, 2, 2, 1, 1, 3};
    final double[] dst = new double[10];

    final int remaining = AbstractBaselineCorrector.removeRangesFromArray(List.of(r), 10, src,
        dst);

    Assertions.assertEquals(8, remaining);
    Assertions.assertArrayEquals(new double[]{2, 2, 2, 2, 2, 2, 2, 3, 0, 0}, dst);
  }

  @Test
  public void testRangeInMiddle() {
    SimpleIndexRange r = new SimpleIndexRange(4, 6);

    final double[] src = new double[]{2, 2, 2, 2, 1, 1, 1, 2, 2, 2};
    final double[] dst = new double[10];

    final int remaining = AbstractBaselineCorrector.removeRangesFromArray(List.of(r), 10, src,
        dst);

    Assertions.assertEquals(7, remaining);
    Assertions.assertArrayEquals(new double[]{2, 2, 2, 2, 2, 2, 2, 0, 0, 0}, dst);
  }

  @Test
  public void testTwoRangesStart() {
    SimpleIndexRange r1 = new SimpleIndexRange(0, 1);
    SimpleIndexRange r2 = new SimpleIndexRange(4, 6);

    final double[] src = new double[]{3, 1, 2, 2, 1, 1, 1, 4, 4, 4};
    final double[] dst = new double[10];

    final int remaining = AbstractBaselineCorrector.removeRangesFromArray(List.of(r1, r2), 10,
        src, dst);

    Assertions.assertEquals(6, remaining);
    Assertions.assertArrayEquals(new double[]{3, 2, 2, 4, 4, 4, 0, 0, 0, 0}, dst);
  }

  @Test
  public void testTwoRangesEnd() {
    SimpleIndexRange r1 = new SimpleIndexRange(4, 6);
    SimpleIndexRange r2 = new SimpleIndexRange(8, 9);

    final double[] src = new double[]{2, 2, 2, 2, 1, 1, 1, 2, 4, 3};
    final double[] dst = new double[10];

    final int remaining = AbstractBaselineCorrector.removeRangesFromArray(List.of(r1, r2), 10,
        src, dst);

    Assertions.assertEquals(6, remaining);
    Assertions.assertArrayEquals(new double[]{2, 2, 2, 2, 2, 3, 0, 0, 0, 0}, dst);
  }
}
