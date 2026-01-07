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

package io.github.mzmine.util.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class IndexRangeTest {

  @Test
  void findRanges() {
    List<IndexRange> ranges = IndexRange.findRanges(List.of(1, 3, 4, 5, 8, 9));
    assertEquals(3, ranges.size());
    assertEquals(1, ranges.get(0).min());
    assertEquals(1, ranges.get(0).maxInclusive());
    assertEquals(2, ranges.get(0).maxExclusive());

    assertEquals(3, ranges.get(1).min());
    assertEquals(5, ranges.get(1).maxInclusive());
    assertEquals(6, ranges.get(1).maxExclusive());

    assertEquals(8, ranges.get(2).min());
    assertEquals(9, ranges.get(2).maxInclusive());
    assertEquals(10, ranges.get(2).maxExclusive());

    ranges = IndexRange.findRanges(List.of());
    assertEquals(0, ranges.size());
  }
}