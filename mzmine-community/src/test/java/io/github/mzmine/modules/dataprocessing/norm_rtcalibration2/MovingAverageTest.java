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

package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MovingAverageTest {

  @Test
  void calculateLinearEqual() {
    double[] data = new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    final double[] average = MovingAverage.calculate(data, 3);
    assertEquals(10, average.length);
    assertArrayEquals(data, average);
    assertEquals(data[0], average[0]);
    assertEquals(data[data.length - 1], average[data.length - 1]);
  }

  @Test
  void calculate() {
    double[] data = new double[]{2, 6, 2, 6, 2, 6, 2, 6, 2, 6};
    final double[] average = MovingAverage.calculate(data, 5);
    assertEquals(10, average.length);
    assertEquals(data[0], average[0]);
    assertEquals(data[data.length - 1], average[data.length - 1]);
  }
}