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

package io.github.mzmine.util;

import java.util.Arrays;

public class ArrayUtils {

  /**
   * Throws out of range exceptions
   *
   * @param arrayLength
   * @param fromIndex
   * @param toIndex
   */
  public static void rangeCheck(int arrayLength, int fromIndex, int toIndex) {
    if (fromIndex > toIndex) {
      throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
    }
    if (fromIndex < 0) {
      throw new ArrayIndexOutOfBoundsException(fromIndex);
    }
    if (toIndex > arrayLength) {
      throw new ArrayIndexOutOfBoundsException(toIndex);
    }
  }

  public static <T> int indexOf(T needle, T[] haystack) {
    for (int i = 0; i < haystack.length; i++) {
      if ((haystack[i] != null && haystack[i].equals(needle)) || (needle == null
                                                                  && haystack[i] == null)) {
        return i;
      }
    }
    return -1;
  }

  public static int indexOf(double needle, double[] haystack) {
    for (int i = 0; i < haystack.length; i++) {
      if(Double.compare(haystack[i], needle) == 0) {
        return i;
      }
    }
    return -1;
  }

  public static double sum(double[] array, int startIndex, int endIndex) {
    assert endIndex < array.length;
    assert startIndex >= 0;

    double sum = 0d;
    for (int i = startIndex; i < endIndex; i++) {
      sum += array[i];
    }
    return sum;
  }

  public static double sum(double[] array) {
    double sum = 0d;
    for (double v : array) {
      sum += v;
    }
    return sum;
  }
  /**
   * @see #smallestDelta(double[], int)
   */
  public static double smallestDelta(double[] values) {
    return smallestDelta(values, values.length);
  }

  /**
   * @param values    the values. sorted ascending/descending
   * @param numValues
   * @return The smallest delta between two neighbouring values. 0 if values contains 0 or 1 value.
   */
  public static double smallestDelta(double[] values, int numValues) {
    if (values.length <= 1) {
      return 0d;
    }

    double prevValue = values[0];
    double smallestDelta = Double.POSITIVE_INFINITY;
    for (int i = 1; i < numValues; i++) {
      final double delta = Math.abs(values[i] - prevValue);
      if (Double.compare(0d, delta) != 0 && delta < smallestDelta) {
        smallestDelta = delta;
      }
      prevValue = values[i];
    }

    return smallestDelta;
  }

  public static void fill2D(double[][] array, double value) {
    for (double[] doubles : array) {
      Arrays.fill(doubles, value);
    }
  }

  /**
   * Reverses the given array.
   * @param input The array.
   */
  public static void reverse(int[] input) {
    int last = input.length - 1;
    int middle = input.length / 2;
    for (int i = 0; i <= middle; i++) {
      int temp = input[i];
      input[i] = input[last - i];
      input[last - i] = temp;
    }
  }

  public static void reverse(double[] input) {
    int last = input.length - 1;
    int middle = input.length / 2;
    for (int i = 0; i <= middle; i++) {
      double temp = input[i];
      input[i] = input[last - i];
      input[last - i] = temp;
    }
  }

  public static double lastElement(double[] array) {
    assert array.length > 0;
    return array[array.length - 1];
  }
}
