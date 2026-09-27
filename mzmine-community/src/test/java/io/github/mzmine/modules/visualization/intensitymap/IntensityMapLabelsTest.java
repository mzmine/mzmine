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

package io.github.mzmine.modules.visualization.intensitymap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Range;
import org.junit.jupiter.api.Test;

class IntensityMapLabelsTest {

  @Test
  void textJoinsNameAndIon() {
    assertEquals("Caffeine [M+H]+", IntensityMapLabels.text("Caffeine", "[M+H]+", "195.0877"));
  }

  @Test
  void rangesFallBackToTheApex() {
    assertEquals(Range.closed(4.5, 4.75), IntensityMapLabels.range(Range.closed(4.5f, 4.75f), 4.6));
    assertEquals(Range.singleton(4.6), IntensityMapLabels.range(null, 4.6));
  }

  @Test
  void textFallsBackToMz() {
    assertEquals("m/z 195.0877 [M+Na]+", IntensityMapLabels.text(null, "[M+Na]+", "195.0877"));
    assertEquals("m/z 195.0877", IntensityMapLabels.text(" ", null, "195.0877"));
  }
}
