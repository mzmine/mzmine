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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class IndexRangesListTest {

  @Test
  void containsSingleAndRange() {
    final IndexRangesList ranges = IndexRangesList.parse("1,5-6");

    assertTrue(ranges.contains(1));
    assertTrue(ranges.contains(5));
    assertTrue(ranges.contains(6));

    assertFalse(ranges.contains(2));
    assertFalse(ranges.contains(4));
    assertFalse(ranges.contains(7));
  }

  @Test
  void containsNullAndBlank() {
    final IndexRangesList ranges = IndexRangesList.parse("1,5-6");
    assertFalse(ranges.contains((Integer) null));

    final IndexRangesList blank = IndexRangesList.parse("");
    assertTrue(blank.isEmpty());
    assertFalse(blank.contains(1));
  }

  @Test
  void roundTripAsString() {
    assertEquals("1,5-6", IndexRangesList.parse("1,5-6").asString());
    assertEquals("1,5-6", IndexRangesList.parse(" 1 , 5-6 ").asString());
  }

  @Test
  void emptyDefaults() {
    final IndexRangesList ranges = new IndexRangesList(List.of());
    assertTrue(ranges.isEmpty());
    assertEquals("", ranges.asString());
  }

  @Test
  void sortsRangesAscending() {
    assertEquals("1,5-6,10", IndexRangesList.parse("10,5-6,1").asString());
  }

  @Test
  void mergesOverlappingRanges() {
    // overlapping ranges are combined into one
    assertEquals("1-8", IndexRangesList.parse("1-5,3-8").asString());
    // a range fully contained in another disappears
    assertEquals("1-10", IndexRangesList.parse("1-10,3-8").asString());
    // shared endpoint still overlaps
    assertEquals("1-8", IndexRangesList.parse("1-5,5-8").asString());
  }

  @Test
  void removesDuplicates() {
    assertEquals("5", IndexRangesList.parse("5,5,5").asString());
    assertEquals("1,5", IndexRangesList.parse("5,1,5,1").asString());
  }

  @Test
  void keepsAdjacentRangesSeparate() {
    // 1-3 and 4-6 do not intersect (no shared index), so they are not merged
    assertEquals("1-3,4-6", IndexRangesList.parse("1-3,4-6").asString());
  }

  @Test
  void dropsEmptyRanges() {
    // 5-2 is an empty range (end < start) and is dropped
    assertEquals("3", IndexRangesList.parse("5-2,3").asString());
  }

  @Test
  void containsAcrossMergedRange() {
    final IndexRangesList ranges = IndexRangesList.parse("1-5,3-8");
    assertTrue(ranges.contains(1));
    assertTrue(ranges.contains(7));
    assertTrue(ranges.contains(8));
    assertFalse(ranges.contains(0));
    assertFalse(ranges.contains(9));
  }

  @Test
  void binarySearchAcrossManyRanges() {
    final IndexRangesList ranges = IndexRangesList.parse("1,5-6,10-12,20,30-31");
    assertTrue(ranges.contains(1));
    assertTrue(ranges.contains(11));
    assertTrue(ranges.contains(20));
    assertTrue(ranges.contains(31));

    assertFalse(ranges.contains(2));
    assertFalse(ranges.contains(7));
    assertFalse(ranges.contains(13));
    assertFalse(ranges.contains(21));
    assertFalse(ranges.contains(32));
  }
}
