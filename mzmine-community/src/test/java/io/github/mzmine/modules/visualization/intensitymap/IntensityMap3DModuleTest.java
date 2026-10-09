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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntensityMap3DModuleTest {

  @Test
  void colocatedRangesKeepTheOrderOfSimilarity() {
    final List<Range<Double>> ranges = new ArrayList<>();
    // the selected feature, then co-located features by decreasing similarity
    for (final Range<Double> range : List.of(Range.closed(760.58, 760.59),
        Range.closed(734.56, 734.57), Range.closed(798.54, 798.55),
        Range.closed(760.585, 760.60))) {
      IntensityMap3DModule.addMerged(ranges, range);
    }
    // an overlapping range extends the earlier one at its position
    assertEquals(List.of(Range.closed(760.58, 760.60), Range.closed(734.56, 734.57),
        Range.closed(798.54, 798.55)), ranges);
  }
}
