/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.msdk;

import java.util.Arrays;

import org.jetbrains.annotations.NotNull;

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
  public static double calcQuantile(double[] values, @NotNull Integer size, double q) {

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
