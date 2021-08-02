/*
 * (C) Copyright 2015-2017 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */

package io.github.msdk.util;

import javax.annotation.Nonnull;

/**
 * Array utilities
 */
public class ArrayUtil {

  /**
   * <p>
   * addToArray.
   * </p>
   *
   * @param array an array of double.
   * @param value a double.
   * @param pos a int.
   * @return an array of double.
   */
  public static @Nonnull double[] addToArray(@Nonnull double[] array, double value, int pos) {

    if (pos >= array.length) {
      double newArray[] = new double[pos * 2];
      System.arraycopy(array, 0, newArray, 0, array.length);
      array = newArray;
    }

    array[pos] = value;
    return array;

  }

  /**
   * <p>
   * addToArray.
   * </p>
   *
   * @param array an array of float.
   * @param value a float.
   * @param pos a int.
   * @return an array of float.
   */
  public static @Nonnull float[] addToArray(@Nonnull float[] array, float value, int pos) {

    if (pos >= array.length) {
      float newArray[] = new float[pos * 2];
      System.arraycopy(array, 0, newArray, 0, array.length);
      array = newArray;
    }

    array[pos] = value;
    return array;

  }

}
