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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An immutable list of {@link IndexRange} with a fast {@link #contains(int)} check. Parsed from and
 * formatted to a string of single indices and ranges, e.g. "1,5-6", via {@link IndexRange}.
 * <p>
 * The ranges are normalized in the constructor: empty ranges are dropped, the remaining ranges are
 * sorted ascending by their start, and overlapping ranges are merged into one. The result is a
 * sorted list of disjoint ranges, which {@link #contains(int)} exploits via binary search.
 *
 * @param ranges the normalized, sorted, non-overlapping ranges, never null
 */
public record IndexRangesList(@NotNull List<IndexRange> ranges) {

  public static final IndexRangesList EMPTY = new IndexRangesList(List.of());

  public IndexRangesList {
    ranges = normalize(ranges);
  }

  /**
   * Drop empty ranges, sort ascending by start, and merge overlapping ranges so the result is a
   * sorted list of disjoint ranges.
   */
  private static @NotNull List<IndexRange> normalize(@Nullable final List<IndexRange> input) {
    if (input == null || input.isEmpty()) {
      return List.of();
    }

    final List<IndexRange> sorted = new ArrayList<>(input.size());
    for (final IndexRange range : input) {
      if (range != null && range.notEmpty()) {
        sorted.add(range);
      }
    }
    if (sorted.isEmpty()) {
      return List.of();
    }
    sorted.sort(
        Comparator.comparingInt(IndexRange::min).thenComparingInt(IndexRange::maxInclusive));

    final List<IndexRange> merged = new ArrayList<>(sorted.size());
    int curMin = sorted.getFirst().min();
    int curMax = sorted.getFirst().maxInclusive();
    for (int i = 1; i < sorted.size(); i++) {
      final IndexRange range = sorted.get(i);
      // sorted by min, so range.min() >= curMin. They overlap when range starts within [curMin,
      // curMax]; adjacent (non-overlapping) ranges are kept separate.
      if (range.min() <= curMax) {
        curMax = Math.max(curMax, range.maxInclusive());
      } else {
        merged.add(IndexRange.ofInclusive(curMin, curMax));
        curMin = range.min();
        curMax = range.maxInclusive();
      }
    }
    merged.add(IndexRange.ofInclusive(curMin, curMax));
    return List.copyOf(merged);
  }

  /**
   * Parse a string of ranges like "1,4-9,11,14-15" into an {@link IndexRangesList}.
   *
   * @param input string containing ranges separated by commas
   * @return the parsed list, empty for blank input
   * @throws IllegalArgumentException if the input format is invalid
   */
  public static @NotNull IndexRangesList parse(@Nullable final String input) {
    return new IndexRangesList(IndexRange.parseRanges(input));
  }

  /**
   * Binary search over the sorted, disjoint ranges.
   *
   * @return true if any contained range includes the value
   */
  public boolean contains(final int value) {
    int low = 0;
    int high = ranges.size() - 1;
    while (low <= high) {
      final int mid = (low + high) >>> 1;
      final IndexRange range = ranges.get(mid);
      if (value < range.min()) {
        high = mid - 1;
      } else if (value > range.maxInclusive()) {
        low = mid + 1;
      } else {
        return true;
      }
    }
    return false;
  }

  /**
   * @return true if value is non-null and any contained range includes it
   */
  public boolean contains(@Nullable final Integer value) {
    return value != null && contains(value.intValue());
  }

  public boolean isEmpty() {
    return ranges.isEmpty();
  }

  public boolean notEmpty() {
    return !isEmpty();
  }

  /**
   * @return string representation like "1,5-6" that can be parsed back via {@link #parse(String)}
   */
  public @NotNull String asString() {
    return IndexRange.asString(ranges);
  }

  @Override
  public String toString() {
    return asString();
  }
}
