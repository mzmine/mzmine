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

package io.github.mzmine.modules.visualization.intensitymap.plot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import javafx.geometry.Point2D;
import org.junit.jupiter.api.Test;

class IntensityMapTraceLayerTest {

  @Test
  void flatOutlinesAreWidenedAroundTheirCenter() {
    // a narrow range: 100 px wide, 2 px high
    final List<Point2D> flat = List.of(new Point2D(0, 49), new Point2D(100, 49),
        new Point2D(100, 51), new Point2D(0, 51));

    final List<Point2D> widened = IntensityMapTraceLayer.widened(flat);

    assertEquals(45, widened.get(0).getY(), 1e-9);
    assertEquals(55, widened.get(2).getY(), 1e-9);
    assertEquals(100, widened.get(1).getX(), 1e-9);
  }

  @Test
  void largeOutlinesStay() {
    final List<Point2D> box = List.of(new Point2D(0, 0), new Point2D(50, 0), new Point2D(50, 40),
        new Point2D(0, 40));

    assertSame(box, IntensityMapTraceLayer.widened(box));
  }
}
