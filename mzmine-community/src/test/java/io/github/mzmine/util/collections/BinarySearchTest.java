/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
  void differentSizeBinarySearch() {

    for (int i = 0; i < 6; i++) {
      final double[] data = IntStream.range(0, i + 1).mapToDouble(v -> (double) v).toArray();

      // check to find each element +- 2
      for (int item = -2; item < i + 2; item++) {
        int expectedClosest = MathUtils.withinBounds(item, 0, i + 1);
        int expectedGREQ = item > i ? -1 : MathUtils.withinBounds(item, 0, i + 1);
        int expectedLEQ = item < 0 ? -1 : MathUtils.withinBounds(item, 0, i + 1);
        // will test smaller than item
        int expectedInsertionPoint = switch (item) {
          case int d when d <= 0 -> -1;
          case int d when d > data.length -> -(data.length + 1);
          default -> -(item + 1);
        };

        assertEquals(expectedClosest,
            BinarySearch.binarySearch(data, item + 0.1, DefaultTo.CLOSEST_VALUE));
        assertEquals(expectedClosest,
            BinarySearch.binarySearch(data, item - 0.1, DefaultTo.CLOSEST_VALUE));
        assertEquals(expectedGREQ,
            BinarySearch.binarySearch(data, item - 0.5d, DefaultTo.GREATER_EQUALS));
        assertEquals(expectedLEQ,
            BinarySearch.binarySearch(data, item + 0.5d, DefaultTo.LESS_EQUALS));

        assertEquals(expectedInsertionPoint,
            BinarySearch.binarySearch(data, item - 0.1, DefaultTo.MINUS_INSERTION_POINT),
            "for value %f".formatted(item - 0.1));

        // search within range
        int min = 1;
        int max = 2;
        if (data.length >= max) {
          int expectedGREQ2 = item >= max ? -1 : MathUtils.withinBounds(item, min, max);
          int expectedLEQ2 = item < min ? -1 : MathUtils.withinBounds(item, min, max);
          assertEquals(MathUtils.withinBounds(expectedClosest, min, max),
              BinarySearch.binarySearch(data, item + 0.1, DefaultTo.CLOSEST_VALUE, min, max),
              "for value %f".formatted(item + 0.1));
          assertEquals(MathUtils.withinBounds(expectedClosest, min, max),
              BinarySearch.binarySearch(data, item - 0.1, DefaultTo.CLOSEST_VALUE, min, max),
              "for value %f".formatted(item - 0.1));
          assertEquals(expectedGREQ2,
              BinarySearch.binarySearch(data, item - 0.5d, DefaultTo.GREATER_EQUALS, min, max),
              "for value %f".formatted(item - 0.5));
          assertEquals(expectedLEQ2,
              BinarySearch.binarySearch(data, item + 0.5d, DefaultTo.LESS_EQUALS, min, max),
              "for value %f".formatted(item + 0.5));
        }

        min = 2;
        max = 4;
        if (data.length >= max) {
          int expectedGREQ2 = item >= max ? -1 : MathUtils.withinBounds(item, min, max);
          int expectedLEQ2 = item < min ? -1 : MathUtils.withinBounds(item, min, max);
          assertEquals(MathUtils.withinBounds(expectedClosest, min, max),
              BinarySearch.binarySearch(data, item + 0.1, DefaultTo.CLOSEST_VALUE, min, max),
              "for value %f".formatted(item + 0.1));
          assertEquals(MathUtils.withinBounds(expectedClosest, min, max),
              BinarySearch.binarySearch(data, item - 0.1, DefaultTo.CLOSEST_VALUE, min, max),
              "for value %f".formatted(item - 0.1));
          assertEquals(expectedGREQ2,
              BinarySearch.binarySearch(data, item - 0.5d, DefaultTo.GREATER_EQUALS, min, max),
              "for value %f".formatted(item - 0.5));
          assertEquals(expectedLEQ2,
              BinarySearch.binarySearch(data, item + 0.5d, DefaultTo.LESS_EQUALS, min, max),
              "for value %f".formatted(item + 0.5));
        }
      }
    }
  }

  @Test
  void binarySearchSize1() {
    double[] data = new double[]{5.5};
    assertEquals(0, BinarySearch.binarySearch(data, 5.5, DefaultTo.CLOSEST_VALUE));
    assertEquals(0, BinarySearch.binarySearch(data, 5.6, DefaultTo.CLOSEST_VALUE));
    assertEquals(0, BinarySearch.binarySearch(data, 5.4, DefaultTo.CLOSEST_VALUE));
    assertEquals(0, BinarySearch.binarySearch(data, 5.5, DefaultTo.LESS_EQUALS));
    assertEquals(0, BinarySearch.binarySearch(data, 5.6, DefaultTo.LESS_EQUALS));
    assertEquals(0, BinarySearch.binarySearch(data, 5.5, DefaultTo.GREATER_EQUALS));
    assertEquals(0, BinarySearch.binarySearch(data, 5.4, DefaultTo.GREATER_EQUALS));
    assertEquals(-1, BinarySearch.binarySearch(data, 5.4, DefaultTo.LESS_EQUALS));
    assertEquals(-1, BinarySearch.binarySearch(data, 5.6, DefaultTo.GREATER_EQUALS));
  }

  @Test
  void binarySearchSizeMultiList() {
    // three concat lists of 1,5,10   3   2,4,6
    double[] data = new double[]{1d, 5d, 10d, 3d, 2d, 4d, 6d};
    assertEquals(3, BinarySearch.binarySearch(data, 3, DefaultTo.GREATER_EQUALS, 3, 4));
    assertEquals(3, BinarySearch.binarySearch(data, 2, DefaultTo.GREATER_EQUALS, 3, 4));
    assertEquals(3, BinarySearch.binarySearch(data, 4, DefaultTo.LESS_EQUALS, 3, 4));
    assertEquals(3, BinarySearch.binarySearch(data, 4, DefaultTo.CLOSEST_VALUE, 3, 4));
    assertEquals(3, BinarySearch.binarySearch(data, 2, DefaultTo.CLOSEST_VALUE, 3, 4));
    assertEquals(3, BinarySearch.binarySearch(data, 3, DefaultTo.CLOSEST_VALUE, 3, 4));
    assertEquals(-1, BinarySearch.binarySearch(data, 2, DefaultTo.LESS_EQUALS, 3, 4));
    assertEquals(-1, BinarySearch.binarySearch(data, 4, DefaultTo.GREATER_EQUALS, 3, 4));
    // empty from to range
    assertEquals(-1, BinarySearch.binarySearch(data, 2, DefaultTo.GREATER_EQUALS, 3, 3));

    final double[] data2 = new double[]{
        // first list
        381.4514740245013, 643.5060908298985, 1048.4363122015466,
        // second list
        356.21185091105644, 1237.2631353228137};

    assertEquals(2,
        BinarySearch.binarySearch(data2, 155.10211608911706, DefaultTo.CLOSEST_VALUE, 2, 3));
    assertEquals(0,
        BinarySearch.binarySearch(data2, 155.10211608911706, DefaultTo.CLOSEST_VALUE, 0, 3));
    assertEquals(3,
        BinarySearch.binarySearch(data2, 155.10211608911706, DefaultTo.CLOSEST_VALUE, 3, 5));

    assertEquals(2,
        BinarySearch.binarySearch(data2, 155.10211608911706, DefaultTo.GREATER_EQUALS, 2, 3));
    assertEquals(0,
        BinarySearch.binarySearch(data2, 155.10211608911706, DefaultTo.GREATER_EQUALS, 0, 3));
    assertEquals(3,
        BinarySearch.binarySearch(data2, 155.10211608911706, DefaultTo.GREATER_EQUALS, 3, 5));

    assertEquals(2,
        BinarySearch.binarySearch(data2, 1550.10211608911706, DefaultTo.LESS_EQUALS, 2, 3));
    assertEquals(2,
        BinarySearch.binarySearch(data2, 1550.10211608911706, DefaultTo.LESS_EQUALS, 0, 3));
    assertEquals(4,
        BinarySearch.binarySearch(data2, 1505.10211608911706, DefaultTo.LESS_EQUALS, 3, 5));

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