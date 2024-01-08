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

import com.google.common.collect.Range;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import java.util.function.IntToDoubleFunction;
import org.jetbrains.annotations.NotNull;

/**
 * Easy way to apply binary search on sorted data
 */
public class BinarySearch {

  /**
   * Searches for the value - or the closest available. Copied from
   * {@link Arrays#binarySearch(double[], double)}
   *
   * @param data  data source
   * @param range search for index of lower and upper bound both always included!
   * @return an {@link IndexRange} that may be empty but contains all values within range - always
   * including both bounds
   */
  public static @NotNull IndexRange indexRange(double[] data, Range<Double> range) {
    return indexRange(range, 0, data.length, index -> data[index]);
  }

  /**
   * Searches for the value - or the closest available. Copied from
   * {@link Arrays#binarySearch(double[], double)}
   *
   * @param data  data source
   * @param range search for index of lower and upper bound both always included!
   * @return an {@link IndexRange} that may be empty but contains all values within range - always
   * including both bounds
   */
  public static @NotNull IndexRange indexRange(DoubleList data, Range<Double> range) {
    return indexRange(range, 0, data.size(), data::getDouble);
  }

  /**
   * Searches for the value - or the closest available. Copied from
   * {@link Arrays#binarySearch(double[], double)}
   *
   * @param data       data source
   * @param lowerValue search for index of lower and upper bound both always included!
   * @param upperValue search for index of lower and upper bound both always included!
   * @return an {@link IndexRange} that may be empty but contains all values within range - always
   * including both bounds
   */
  public static @NotNull IndexRange indexRange(double[] data, final double lowerValue,
      final double upperValue) {
    return indexRange(lowerValue, upperValue, 0, data.length, index -> data[index]);
  }

  /**
   * Searches for the value - or the closest available. Copied from
   * {@link Arrays#binarySearch(double[], double)}
   *
   * @param data       data source
   * @param lowerValue search for index of lower and upper bound both always included!
   * @param upperValue search for index of lower and upper bound both always included!
   * @return an {@link IndexRange} that may be empty but contains all values within range - always
   * including both bounds
   */
  public static @NotNull IndexRange indexRange(DoubleList data, final double lowerValue,
      final double upperValue) {
    return indexRange(lowerValue, upperValue, 0, data.size(), data::getDouble);
  }

  /**
   * Searches for the value - or the closest available. Copied from
   * {@link Arrays#binarySearch(double[], double)}
   *
   * @param range                search for index of lower and upper bound both always included!
   * @param totalValues          total number of values (collection size or array length)
   * @param valueAtIndexProvider a function to compute or return the value at an index
   * @return an {@link IndexRange} that may be empty but contains all values within range - always
   * including both bounds
   */
  public static @NotNull IndexRange indexRange(Range<Double> range, int totalValues,
      IntToDoubleFunction valueAtIndexProvider) {
    return indexRange(range, 0, totalValues, valueAtIndexProvider);
  }

  /**
   * Searches for the value - or the closest available. Copied from
   * {@link Arrays#binarySearch(double[], double)}
   *
   * @param range                search for index of lower and upper bound both always included!
   * @param fromIndex            inclusive lower end
   * @param toIndexExclusive     exclusive upper end
   * @param valueAtIndexProvider a function to compute or return the value at an index
   * @return an {@link IndexRange} that may be empty but contains all values within range - always
   * including both bounds
   */
  public static @NotNull IndexRange indexRange(final Range<Double> range, final int fromIndex,
      final int toIndexExclusive, final IntToDoubleFunction valueAtIndexProvider) {
    return indexRange(range.lowerEndpoint(), range.upperEndpoint(), fromIndex, toIndexExclusive,
        valueAtIndexProvider);
  }

  /**
   * Searches for the value - or the closest available. Copied from
   * {@link Arrays#binarySearch(double[], double)}
   *
   * @param lowerValue           search for index of lower and upper bound both always included!
   * @param upperValue           search for index of lower and upper bound both always included!
   * @param totalValues          total number of values (collection size or array length)
   * @param valueAtIndexProvider a function to compute or return the value at an index
   * @return an {@link IndexRange} that may be empty but contains all values within range - always
   * including both bounds
   */
  public static @NotNull IndexRange indexRange(final double lowerValue, final double upperValue,
      final int totalValues, final IntToDoubleFunction valueAtIndexProvider) {
    return indexRange(lowerValue, upperValue, 0, totalValues, valueAtIndexProvider);
  }

  /**
   * Searches for the value - or the closest available. Copied from
   * {@link Arrays#binarySearch(double[], double)}
   *
   * @param lowerValue           search for index of lower and upper bound both always included!
   * @param upperValue           search for index of lower and upper bound both always included!
   * @param fromIndex            inclusive lower end
   * @param toIndexExclusive     exclusive upper end
   * @param valueAtIndexProvider a function to compute or return the value at an index
   * @return an {@link IndexRange} that may be empty but contains all values within range - always
   * including both bounds
   */
  public static @NotNull IndexRange indexRange(final double lowerValue, final double upperValue,
      final int fromIndex, final int toIndexExclusive,
      final IntToDoubleFunction valueAtIndexProvider) {
    assert lowerValue <= upperValue;
    int lower = BinarySearch.binarySearch(lowerValue, DefaultTo.GREATER_EQUALS, fromIndex,
        toIndexExclusive, valueAtIndexProvider);
    if (lower == -1) {
      // no signal found
      return EmptyIndexRange.INSTANCE;
    }

    // start to search from lower index
    int upper = BinarySearch.binarySearch(upperValue, DefaultTo.LESS_EQUALS, lower,
        toIndexExclusive, valueAtIndexProvider);
    return IndexRangeFactory.create(lower, upper);
  }

  /**
   * Searches for the value - or the closest available. Copied from
   * {@link Arrays#binarySearch(double[], double)}
   *
   * @param data           data source
   * @param value          search for this value
   * @param noMatchDefault option to handle a missing value
   * @return this index of the given value or the closest available value if checked. index of the
   * search key, if it is contained in the array; otherwise, (-(insertion point) - 1). The insertion
   * point is defined as the point at which the key would be inserted into the array: the index of
   * the first element greater than the key, or a.length if all elements in the array are less than
   * the specified key. Note that this guarantees that the return value will be >= 0 if and only if
   * the key is found.
   */
  public static int binarySearch(double[] data, double value, @NotNull DefaultTo noMatchDefault) {
    return binarySearch(value, noMatchDefault, 0, data.length, index -> data[index]);
  }

  /**
   * Searches for the value - or the closest available. Copied from
   * {@link Arrays#binarySearch(double[], double)}
   *
   * @param data           data source
   * @param value          search for this value
   * @param noMatchDefault option to handle a missing value
   * @return this index of the given value or the closest available value if checked. index of the
   * search key, if it is contained in the array; otherwise, (-(insertion point) - 1). The insertion
   * point is defined as the point at which the key would be inserted into the array: the index of
   * the first element greater than the key, or a.length if all elements in the array are less than
   * the specified key. Note that this guarantees that the return value will be >= 0 if and only if
   * the key is found.
   */
  public static int binarySearch(DoubleList data, double value, @NotNull DefaultTo noMatchDefault) {
    return binarySearch(value, noMatchDefault, 0, data.size(), data::getDouble);
  }

  /**
   * Searches for the value - or the closest available. Copied from
   * {@link Arrays#binarySearch(double[], double)}
   *
   * @param value                search for this value
   * @param noMatchDefault       option to handle a missing value
   * @param totalValues          total number of values (collection size or array length)
   * @param valueAtIndexProvider a function to compute or return the value at an index
   * @return this index of the given value or the closest available value if checked. index of the
   * search key, if it is contained in the array; otherwise, (-(insertion point) - 1). The insertion
   * point is defined as the point at which the key would be inserted into the array: the index of
   * the first element greater than the key, or a.length if all elements in the array are less than
   * the specified key. Note that this guarantees that the return value will be >= 0 if and only if
   * the key is found.
   */
  public static int binarySearch(double value, @NotNull DefaultTo noMatchDefault, int totalValues,
      IntToDoubleFunction valueAtIndexProvider) {
    return binarySearch(value, noMatchDefault, 0, totalValues, valueAtIndexProvider);
  }

  /**
   * Searches for the value - or the closest available. Copied from
   * {@link Arrays#binarySearch(double[], double)}
   *
   * @param value                search for this value
   * @param noMatchDefault       option to handle missing value
   * @param fromIndex            inclusive lower end
   * @param toIndexExclusive     exclusive upper end
   * @param valueAtIndexProvider a function to compute or return the value at an index
   * @return this index of the given value or the closest available value if checked. index of the
   * search key, if it is contained in the array; otherwise, (-(insertion point) - 1). The insertion
   * point is defined as the point at which the key would be inserted into the array: the index of
   * the first element greater than the key, or a.length if all elements in the array are less than
   * the specified key. Note that this guarantees that the return value will be >= 0 if and only if
   * the key is found.
   */
  public static int binarySearch(double value, @NotNull DefaultTo noMatchDefault, int fromIndex,
      int toIndexExclusive, IntToDoubleFunction valueAtIndexProvider) {
    if (toIndexExclusive == 0) {
      return -1;
    }

    int low = fromIndex;
    int high = toIndexExclusive - 1;

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

    // key not found.
    return switch (noMatchDefault) {
      case CLOSEST_VALUE -> closestValue(value, toIndexExclusive, valueAtIndexProvider, low);
      case MINUS_INSERTION_POINT -> -(low + 1);
      case GREATER_EQUALS ->
          greaterEquals(value, fromIndex, toIndexExclusive, valueAtIndexProvider, low);
      case LESS_EQUALS -> lessEquals(value, fromIndex, toIndexExclusive, valueAtIndexProvider, low);
    };
  }

  private static int closestValue(final double value, final int toIndex,
      final IntToDoubleFunction valueAtIndexProvider, final int index) {
    if (index >= toIndex) {
      return toIndex - 1;
    }
    // might be higher or lower
    final double adjacentValue = valueAtIndexProvider.applyAsDouble(index);
    // check for closest distance to value
    if (adjacentValue <= value && index + 1 < toIndex) {
      final double higherValue = valueAtIndexProvider.applyAsDouble(index + 1);
      return (Math.abs(value - adjacentValue) <= Math.abs(higherValue - value)) ? index : index + 1;
    } else if (adjacentValue > value && index - 1 >= 0) {
      final double lowerValue = valueAtIndexProvider.applyAsDouble(index - 1);
      return (Math.abs(value - adjacentValue) <= Math.abs(lowerValue - value)) ? index : index - 1;
    } else {
      // there was only one data point
      return index;
    }
  }

  private static int lessEquals(final double value, final int fromIndex, final int toIndex,
      final IntToDoubleFunction valueAtIndexProvider, final int index) {
    // toIndex exclusive
    if (index >= toIndex) {
      return toIndex - 1;  // last value
    }
    // might be higher or lower
    final double adjacentValue = valueAtIndexProvider.applyAsDouble(index);
    if (adjacentValue <= value) {
      return index;
    } else if (index <= fromIndex) {
      return -1;  // out of range
    } else {
      return index - 1;
    }
  }

  private static int greaterEquals(final double value, final int fromIndex, final int toIndex,
      final IntToDoubleFunction valueAtIndexProvider, final int index) {
    if (index >= toIndex) {
      return -1;
    }
    if (index < fromIndex) {
      return fromIndex;
    }
    // might be higher or lower
    final double adjacentValue = valueAtIndexProvider.applyAsDouble(index);
    if (adjacentValue >= value) {
      return index;
    } else if (index + 1 >= toIndex) {
      return -1;
    } else {
      return index + 1;
    }
  }

  /**
   * Default value returned if a direct match was missing
   */
  public enum DefaultTo {
    CLOSEST_VALUE, MINUS_INSERTION_POINT, GREATER_EQUALS, LESS_EQUALS;
  }

}
