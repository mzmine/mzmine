/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BinarySearchTest {

  static final int MAX = 9;
  static final int MIN = 0;
  double[] data;

  @BeforeEach
  void init() {
    data = IntStream.range(MIN, MAX + 1).mapToDouble(value -> (double) value).toArray();
  }

  @Test
  void indexRange() {
    checkIndexRange(-10, -1);
    checkIndexRange(-10, 0);
    checkIndexRange(0, 3);
    checkIndexRange(-1, 2);
    checkIndexRange(0.5, 2);
    checkIndexRange(0.5, 1);
    checkIndexRange(0.5, 10);
    checkIndexRange(0.5, 115);
    checkIndexRange(8, 115);
    checkIndexRange(9, 115);
    checkIndexRange(100, 115);
  }

  void checkIndexRange(double min, double max) {
    IndexRange range = BinarySearch.indexRange(data, min, max);
    int upper = Math.min(MAX, (int) Math.floor(max));
    int lower = Math.max(MIN, (int) Math.ceil(min));
    int size = upper < 0 || upper < lower ? 0 : upper - lower + 1;
    assertEquals(size, range.size());
    if (range.isEmpty()) {
      return;
    }

    assertEquals(lower, range.min());
    assertEquals(upper, range.maxInclusive());
    assertEquals(upper + 1, range.maxExclusive());
  }

  @Test
  void binarySearchClosest() {
    assertEquals(0, BinarySearch.binarySearch(data, -1, DefaultTo.CLOSEST_VALUE));
    assertEquals(1, BinarySearch.binarySearch(data, 0.5, DefaultTo.CLOSEST_VALUE));
    assertEquals(1, BinarySearch.binarySearch(data, 0.6, DefaultTo.CLOSEST_VALUE));
    assertEquals(9, BinarySearch.binarySearch(data, 9.6, DefaultTo.CLOSEST_VALUE));
    assertEquals(9, BinarySearch.binarySearch(data, 99.6, DefaultTo.CLOSEST_VALUE));
    assertEquals(9, BinarySearch.binarySearch(data, 8.9, DefaultTo.CLOSEST_VALUE));
    assertEquals(5, BinarySearch.binarySearch(data, 5, DefaultTo.CLOSEST_VALUE));
    assertEquals(5, BinarySearch.binarySearch(data, 5.01, DefaultTo.CLOSEST_VALUE));
  }

  @Test
  void binarySearchGreaterEq() {
    assertEquals(0, BinarySearch.binarySearch(data, -1, DefaultTo.GREATER_EQUALS));
    assertEquals(1, BinarySearch.binarySearch(data, 0.5, DefaultTo.GREATER_EQUALS));
    assertEquals(1, BinarySearch.binarySearch(data, 0.6, DefaultTo.GREATER_EQUALS));
    assertEquals(-1, BinarySearch.binarySearch(data, 9.6, DefaultTo.GREATER_EQUALS));
    assertEquals(-1, BinarySearch.binarySearch(data, 99.6, DefaultTo.GREATER_EQUALS));
    assertEquals(9, BinarySearch.binarySearch(data, 8.9, DefaultTo.GREATER_EQUALS));
    assertEquals(5, BinarySearch.binarySearch(data, 5, DefaultTo.GREATER_EQUALS));
    assertEquals(6, BinarySearch.binarySearch(data, 5.01, DefaultTo.GREATER_EQUALS));
  }

  @Test
  void binarySearchLowerEq() {
    assertEquals(-1, BinarySearch.binarySearch(data, -1, DefaultTo.LESS_EQUALS));
    assertEquals(0, BinarySearch.binarySearch(data, 0.5, DefaultTo.LESS_EQUALS));
    assertEquals(0, BinarySearch.binarySearch(data, 0.6, DefaultTo.LESS_EQUALS));
    assertEquals(9, BinarySearch.binarySearch(data, 9.6, DefaultTo.LESS_EQUALS));
    assertEquals(9, BinarySearch.binarySearch(data, 99.6, DefaultTo.LESS_EQUALS));
    assertEquals(8, BinarySearch.binarySearch(data, 8.9, DefaultTo.LESS_EQUALS));
    assertEquals(5, BinarySearch.binarySearch(data, 5, DefaultTo.LESS_EQUALS));
    assertEquals(5, BinarySearch.binarySearch(data, 5.01, DefaultTo.LESS_EQUALS));
  }

  @Test
  void binarySearchDirectMatch() {
    assertEquals(-1, BinarySearch.binarySearch(data, -1, DefaultTo.MINUS_INSERTION_POINT));
    assertEquals(-2, BinarySearch.binarySearch(data, 0.5, DefaultTo.MINUS_INSERTION_POINT));
    assertEquals(-11, BinarySearch.binarySearch(data, 99.6, DefaultTo.MINUS_INSERTION_POINT));
    assertEquals(5, BinarySearch.binarySearch(data, 5, DefaultTo.MINUS_INSERTION_POINT));
    assertEquals(-7, BinarySearch.binarySearch(data, 5.01, DefaultTo.MINUS_INSERTION_POINT));
  }
}