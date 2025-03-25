/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;
import org.jetbrains.annotations.NotNull;

public sealed interface IndexRange permits EmptyIndexRange, SimpleIndexRange, SingleIndexRange {

  static List<IndexRange> findRanges(List<Integer> values) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    List<Integer> sorted = values.stream().filter(Objects::nonNull).sorted().distinct().toList();
    List<IndexRange> ranges = new ArrayList<>();
    // 1    3 4 5     8 9
    Integer first = sorted.getFirst();
    Integer lastInRange = first;

    for (int i = 1; i < sorted.size(); i++) {
      Integer currentNumber = sorted.get(i);

      if (lastInRange + 1 == currentNumber) {
        lastInRange = currentNumber; // consecutive numbers
        continue;
      }
      // gap detected
      ranges.add(IndexRange.ofInclusive(first, lastInRange));
      first = currentNumber;
      lastInRange = first;
    }

    // ADD LAST
    ranges.add(IndexRange.ofInclusive(first, lastInRange));

    return ranges;
  }

  /**
   * Creates an {@link IndexRange} from min to maxInclusive. Use {@link IndexRange#isEmpty()} to
   * check for elements
   *
   * @param min          first index
   * @param maxExclusive is last included index + 1
   * @return an {@link IndexRange} that may be empty
   */
  @NotNull
  static IndexRange ofExclusive(int min, int maxExclusive) {
    return ofInclusive(min, maxExclusive - 1);
  }

  /**
   * Creates an {@link IndexRange} from min to maxInclusive. Use {@link IndexRange#isEmpty()} to
   * check for elements
   *
   * @param min          first index
   * @param maxInclusive last included index
   * @return an {@link IndexRange} that may be empty
   */
  @NotNull
  static IndexRange ofInclusive(int min, int maxInclusive) {
    if (maxInclusive < min || min == -1 || maxInclusive == -1) {
      return EmptyIndexRange.INSTANCE;
    }
    if (maxInclusive == min) {
      return new SingleIndexRange(min);
    }
    return new SimpleIndexRange(min, maxInclusive);
  }

  int size();

  default boolean isEmpty() {
    return size() <= 0;
  }

  default boolean notEmpty() {
    return !isEmpty();
  }

  int min();

  int maxInclusive();

  default int maxExclusive() {
    return maxInclusive() + 1;
  }

  default void forEach(IntConsumer indexConsumer) {
    for (int i = min(); i < maxExclusive(); i++) {
      indexConsumer.accept(i);
    }
  }

  /**
   * Remove the index range from an array
   *
   * @param data source data
   * @return new array with range excluded or the same data if nothing is removed
   */
  default <T> T[] copyRemoveRange(T[] data) {
    if (isEmpty()) {
      return data;
    }
    // all the last elements are within range
    if (maxExclusive() == data.length) {
      return Arrays.copyOfRange(data, 0, min()); // lower points to the first index within range
    } else if (min() == 0) {
      return Arrays.copyOfRange(data, maxExclusive(), data.length);
    } else {
      // concat ranges
      var upperLength = data.length - maxExclusive();
      int newLength = min() + upperLength;
      // copied from Arrays.copyOfRange
      T[] results = CollectionUtils.createArray(data, newLength);
      System.arraycopy(data, 0, results, 0, min());
      System.arraycopy(data, maxExclusive(), results, min(), upperLength);
      return results;
    }
  }

  /**
   * Remove the index range from an array
   *
   * @param data source data
   * @return new array with range excluded or the same data if nothing is removed
   */
  default <T> List<T> copyRemoveRange(List<T> data) {
    if (isEmpty()) {
      return data;
    }
    // all the last elements are within range
    if (maxExclusive() == data.size()) {
      return data.subList(0, min()); // lower points to the first index within range
    } else if (min() == 0) {
      return data.subList(maxExclusive(), data.size());
    } else {
      List<T> list = new ArrayList<>(data.subList(0, min()));
      list.addAll(data.subList(maxExclusive(), data.size()));
      return list;
    }
  }

  default boolean contains(int index) {
    return min() <= index && index <= maxInclusive();
  }

  /**
   * Create a sublist view that contains this IndexRange
   */
  default <T> List<T> sublist(List<T> data) {
    return isEmpty() ? List.of() : data.subList(min(), maxExclusive());
  }

  /**
   * Create a sublist view that contains this IndexRange
   */
  default <T> T[] subarray(T[] data) {
    return isEmpty() ? CollectionUtils.createArray(data, 0)
        : Arrays.copyOfRange(data, min(), maxExclusive());
  }

  /**
   * Create a sublist view that contains this IndexRange
   */
  default double[] subarray(double[] data) {
    return isEmpty() ? new double[0] : Arrays.copyOfRange(data, min(), maxExclusive());
  }

  /**
   * Create a sublist view that contains this IndexRange
   */
  default int[] subarray(int[] data) {
    return isEmpty() ? new int[0] : Arrays.copyOfRange(data, min(), maxExclusive());
  }

  /**
   * Create a sublist view that contains this IndexRange
   */
  default float[] subarray(float[] data) {
    return isEmpty() ? new float[0] : Arrays.copyOfRange(data, min(), maxExclusive());
  }

  /**
   * Create a sublist view that contains this IndexRange
   */
  default long[] subarray(long[] data) {
    return isEmpty() ? new long[0] : Arrays.copyOfRange(data, min(), maxExclusive());
  }

  /**
   * Create a sublist view that contains this IndexRange
   */
  default boolean[] subarray(boolean[] data) {
    return isEmpty() ? new boolean[0] : Arrays.copyOfRange(data, min(), maxExclusive());
  }
}
