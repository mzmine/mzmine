/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

import java.util.Arrays;
import java.util.function.IntToDoubleFunction;

/**
 * Easy way to apply binary search on sorted data
 */
public class BinarySearch {

  /**
   * Searches for the value - or the closest available. Copied from
   * {@link Arrays#binarySearch(double[], double)}
   *
   * @param value                 search for this value
   * @param defaultToClosestValue return the closest value value
   * @param totalValues           total number of values (collection size or array length)
   * @param valueAtIndexProvider  a function to compute or return the value at an index
   * @return this index of the given value or the closest available value if checked. index of the
   * search key, if it is contained in the array; otherwise, (-(insertion point) - 1). The insertion
   * point is defined as the point at which the key would be inserted into the array: the index of
   * the first element greater than the key, or a.length if all elements in the array are less than
   * the specified key. Note that this guarantees that the return value will be >= 0 if and only if
   * the key is found.
   */
  public static int binarySearch(double value, boolean defaultToClosestValue, int totalValues,
      IntToDoubleFunction valueAtIndexProvider) {
    return binarySearch(value, defaultToClosestValue, 0, totalValues, valueAtIndexProvider);
  }


  /**
   * Searches for the value - or the closest available. Copied from
   * {@link Arrays#binarySearch(double[], double)}
   *
   * @param value                 search for this value
   * @param defaultToClosestValue return the closest value value
   * @param fromIndex             inclusive lower end
   * @param toIndex               exclusive upper end
   * @param valueAtIndexProvider  a function to compute or return the value at an index
   * @return this index of the given value or the closest available value if checked. index of the
   * search key, if it is contained in the array; otherwise, (-(insertion point) - 1). The insertion
   * point is defined as the point at which the key would be inserted into the array: the index of
   * the first element greater than the key, or a.length if all elements in the array are less than
   * the specified key. Note that this guarantees that the return value will be >= 0 if and only if
   * the key is found.
   */
  public static int binarySearch(double value, boolean defaultToClosestValue, int fromIndex,
      int toIndex, IntToDoubleFunction valueAtIndexProvider) {
    if (toIndex == 0) {
      return -1;
    }

    int low = fromIndex;
    int high = toIndex - 1;

    while (low <= high) {
      int mid = (low + high) >>> 1; // bit shift by 1 for sum of positive integers = / 2
      double midValue = valueAtIndexProvider.applyAsDouble(mid);

      if (midValue < value) {
        low = mid + 1;  // Neither value is NaN, thisVal is smaller
      } else if (midValue > value) {
        high = mid - 1; // Neither value is NaN, thisVal is larger
      } else {
        long midBits = Double.doubleToLongBits(midValue);
        long keyBits = Double.doubleToLongBits(value);
        if (midBits == keyBits) {
          return mid;  // Key found
        } else if (midBits < keyBits) {
          low = mid + 1;  // (-0.0, 0.0) or (!NaN, NaN)
        } else {
          high = mid - 1;  // (0.0, -0.0) or (NaN, !NaN)
        }
      }
    }
    if (defaultToClosestValue) {
      if (low >= toIndex) {
        return toIndex - 1;
      }
      // might be higher or lower
      final double adjacentValue = valueAtIndexProvider.applyAsDouble(low);
      // check for closest distance to value
      if (adjacentValue <= value && low + 1 < toIndex) {
        final double higherValue = valueAtIndexProvider.applyAsDouble(low + 1);
        return (Math.abs(value - adjacentValue) <= Math.abs(higherValue - value)) ? low : low + 1;
      } else if (adjacentValue > value && low - 1 >= 0) {
        final double lowerValue = valueAtIndexProvider.applyAsDouble(low - 1);
        return (Math.abs(value - adjacentValue) <= Math.abs(lowerValue - value)) ? low : low - 1;
      } else {
        // there was only one data point
        return low;
      }
    }
    return -(low + 1);  // key not found.
  }

}
