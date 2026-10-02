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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class IntensityMapColorsTest {

  @Test
  void clippedRangeSpansTheWholeColorScale() {
    assertEquals(0, IntensityMapColors.clipped(0.1, 0.2, 0.6), 1e-12);
    assertEquals(0, IntensityMapColors.clipped(0.2, 0.2, 0.6), 1e-12);
    assertEquals(0.5, IntensityMapColors.clipped(0.4, 0.2, 0.6), 1e-12);
    assertEquals(1, IntensityMapColors.clipped(0.6, 0.2, 0.6), 1e-12);
    assertEquals(1, IntensityMapColors.clipped(0.9, 0.2, 0.6), 1e-12);
  }

  @Test
  void fullRangeKeepsColorPositions() {
    assertEquals(0.37, IntensityMapColors.clipped(0.37, 0, 1), 1e-12);
  }

  @Test
  void darkBackgroundsAreDetectedByLuminance() {
    assertTrue(IntensityMapColors.isDark(Color.BLACK));
    assertTrue(IntensityMapColors.isDark(Color.web("#1b1f24")));
    assertFalse(IntensityMapColors.isDark(Color.WHITE));
    assertFalse(IntensityMapColors.isDark(Color.web("#f8fafc")));
  }
}
