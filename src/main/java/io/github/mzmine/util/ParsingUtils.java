/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.util;

import java.nio.DoubleBuffer;
import java.util.List;

public class ParsingUtils {

  /**
   * Value separator for storing lists and arrays.
   */
  public static String SEPARATOR = ";";

  public static double[] stringToDoubleArray(String string) {
    final String[] strValues = string.split(ParsingUtils.SEPARATOR);
    final double[] values = new double[strValues.length];
    for (int i = 0; i < strValues.length; i++) {
      values[i] = Double.parseDouble(strValues[i]);
    }
    return values;
  }

  public static String doubleArrayToString(double[] array, int length) {
    assert length <= array.length;

    StringBuilder b = new StringBuilder();
    for (int i = 0; i < length; i++) {
      double v = array[i];
      b.append(v);
      if (i < length - 1) {
        b.append(SEPARATOR);
      }
    }
    return b.toString();
  }

  public static String doubleBufferToString(DoubleBuffer buffer) {
    StringBuilder b = new StringBuilder();
    for (int i = 0, arrayLength = buffer.capacity(); i < arrayLength; i++) {
      double v = buffer.get(i);
      b.append(v);
      if (i < arrayLength - 1) {
        b.append(SEPARATOR);
      }
    }
    return b.toString();
  }

  public static String intArrayToString(int[] array, int length) {
    assert length <= array.length;

    StringBuilder b = new StringBuilder();
    for (int i = 0; i < length; i++) {
      int v = array[i];
      b.append(v);
      if (i < length - 1) {
        b.append(SEPARATOR);
      }
    }
    return b.toString();
  }

  public static int[] stringToIntArray(String string) {
    final String[] strValues = string.split(ParsingUtils.SEPARATOR);
    final int[] values = new int[strValues.length];
    for (int i = 0; i < strValues.length; i++) {
      values[i] = Integer.parseInt(strValues[i]);
    }
    return values;
  }

  public static <T extends Object> int[] getIndicesOfSubListElements(List<T> sublist,
      List<T> fullList) {
    final int[] indices = new int[sublist.size()];

    int rawIndex = fullList.indexOf(sublist.get(0));
    int subListIndex = 0;
    indices[subListIndex] = rawIndex;

    while (subListIndex < sublist.size() && rawIndex < fullList.size()) {
      if (sublist.get(subListIndex)
          .equals(fullList.get(rawIndex))) { // don't compare identity to make robin happy
        indices[subListIndex] = rawIndex;
        subListIndex++;
      }
      rawIndex++;
    }

    if (subListIndex < sublist.size()) {
      throw new IllegalStateException(
          "Incomplete remap. Sublist did not contain all scans in the fullList.");
    }

    return indices;
  }

}
