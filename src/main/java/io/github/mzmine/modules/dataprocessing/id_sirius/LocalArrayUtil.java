/*
 * (C) Copyright 2015-2018 by MSDK Development Team
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

package io.github.mzmine.modules.dataprocessing.id_sirius;

import javax.annotation.Nonnull;

public class LocalArrayUtil {

  /**
   * Method for converting float arrays into doubles
   * @param array
   * @return
   */
  public static @Nonnull double[] convertToDoubles(@Nonnull float[] array) {
    double doubles[] = new double[array.length];
    for (int i = 0; i < array.length; i++)
      doubles[i] = array[i];

    return doubles;
  }

}
