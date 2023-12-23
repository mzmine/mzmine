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

import io.github.mzmine.util.CollectionUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public sealed interface IndexRange permits EmptyIndexRange, SimpleIndexRange, SingleIndexRange {

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
      return Arrays.copyOf(data, min()); // lower points to the first index within range
    } else if (min() == 0) {
      return Arrays.copyOfRange(data, min(), maxExclusive());
    } else {
      // concat ranges
      var upperLength = data.length - maxInclusive();

      int newLength = min() + upperLength;
      // copied from Arrays.copyOfRange
      T[] results = CollectionUtils.createArray(data, newLength);
      System.arraycopy(data, 0, results, 0, min());
      System.arraycopy(data, maxInclusive(), results, min(), upperLength);
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
    List<T> list = new ArrayList<>();
    for (int i = 0; i < data.size(); i++) {
      if (contains(i)) {
        list.add(data.get(i));
      }
    }
    return list;
  }

  default boolean contains(int index) {
    return min() <= index && index <= maxInclusive();
  }

}
