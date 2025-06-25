/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import java.util.Comparator;
import java.util.Objects;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class Comparators {

  public static Comparator<Float> COMPARE_ABS_FLOAT = (a, b) -> {
    if (Objects.equals(a, b)) {
      return 0;
    } else if (a == null) {
      return -1;
    } else if (b == null) {
      return 1;
    } else {
      return Float.compare(Math.abs(a), Math.abs(b));
    }
  };
  public static Comparator<Double> COMPARE_ABS_DOUBLE = (a, b) -> {
    if (Objects.equals(a, b)) {
      return 0;
    } else if (a == null) {
      return -1;
    } else if (b == null) {
      return 1;
    } else {
      return Double.compare(Math.abs(a), Math.abs(b));
    }
  };
  public static Comparator<Number> COMPARE_ABS_NUMBER = (a, b) -> {
    if (Objects.equals(a, b)) {
      return 0;
    } else if (a == null) {
      return -1;
    } else if (b == null) {
      return 1;
    } else {
      return Double.compare(Math.abs(a.doubleValue()), Math.abs(b.doubleValue()));
    }
  };
  public static Comparator<Integer> COMPARE_ABS_INT = (a, b) -> {
    if (Objects.equals(a, b)) {
      return 0;
    } else if (a == null) {
      return -1;
    } else if (b == null) {
      return 1;
    } else {
      return Integer.compare(Math.abs(a), Math.abs(b));
    }
  };

  /**
   * Comparing doubles scores: descending with nulls last usage:
   * stream.sorted(Comparator.comparing(..., Comparators.scoreDescending())
   *
   * @return Comparator for scores
   */
  public static <T extends Comparable<? super T>> Comparator<T> scoreDescending() {
    return Comparator.nullsLast(Comparator.reverseOrder());
  }
}
