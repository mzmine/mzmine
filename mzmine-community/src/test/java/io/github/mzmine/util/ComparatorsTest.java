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

package io.github.mzmine.util;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ComparatorsTest {


  private static final Logger logger = Logger.getLogger(ComparatorsTest.class.getName());

  @Disabled
  @Test
  void testReversed() {
    // general question was if reversing a double comparator works on the whole
    // this compares first on detections, then on intensity and reverses the whole comparison
    final Comparator<TestPoint> reversed = Comparator.comparing(TestPoint::detections)
        .thenComparing(TestPoint::intensity).reversed();

    final List<TestPoint> list = Stream.of(p(2, 2.f), p(1, 5.3f), p(1, 3.f), p(2, 4.f), p(3, 0.3f))
        .sorted(reversed).toList();
    logger.info(list.toString());
  }

  private TestPoint p(int detections, float intensity) {
    return new TestPoint(detections, intensity);
  }
  private record TestPoint(int detections, float intensity) {

  }
}