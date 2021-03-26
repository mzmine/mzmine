/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.util;

public class ArrayUtils {

  public static <T> int indexOf(T needle, T[] haystack) {
    for (int i = 0; i < haystack.length; i++) {
      if ((haystack[i] != null && haystack[i].equals(needle))
          || (needle == null && haystack[i] == null)) {
        return i;
      }
    }
    return -1;
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
}
