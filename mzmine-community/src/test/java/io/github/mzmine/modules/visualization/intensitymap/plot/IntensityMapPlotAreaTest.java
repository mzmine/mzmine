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

import org.junit.jupiter.api.Test;

class IntensityMapPlotAreaTest {

  @Test
  void dataCoveringThePlotAreaStay() {
    assertEquals(0, IntensityMapPlotArea.correction(-50, 900, 0, 800));
  }

  @Test
  void panningStopsAtTheDataEdges() {
    // dragged to the right, the left data edge entered the plot area
    assertEquals(-30, IntensityMapPlotArea.correction(30, 1000, 0, 800));
    // dragged to the left, the right data edge entered the plot area
    assertEquals(20, IntensityMapPlotArea.correction(-300, 780, 0, 800));
  }

  @Test
  void smallDataAreCentered() {
    assertEquals(150, IntensityMapPlotArea.correction(100, 600, 0, 1000));
  }
}
