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

package io.github.mzmine.datamodel.msdk;

import java.util.Arrays;

import javax.annotation.Nonnull;

/**
 * Mathematical calculation-related helper class
 */
public class MathUtils {

  /**
   * Calculates q-quantile value of values. q=0.5 =&gt; median
   *
   * @param values an array of double.
   * @param size a {@link Integer} object.
   * @param q a double.
   * @return a double.
   */
  public static double calcQuantile(double[] values, @Nonnull Integer size, double q) {

    if (size == 0)
      return 0.0;

    if (size == 1)
      return values[0];

    if (q > 1)
      q = 1;

    if (q < 0)
      q = 0;

    // Clone the array and sort it
    double[] vals = new double[size];
    System.arraycopy(values, 0, vals, 0, size);
    Arrays.sort(vals);

    int ind1 = (int) Math.floor((size - 1) * q);
    int ind2 = (int) Math.ceil((size - 1) * q);

    return (vals[ind1] + vals[ind2]) / 2.0;

  }



}
