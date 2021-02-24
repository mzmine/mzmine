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

package io.github.mzmine.datamodel.featuredata;

import io.github.mzmine.datamodel.Scan;
import java.nio.DoubleBuffer;

/**
 * Stores series of m/z values.
 *
 * @author https://github.com/SteffenHeu
 */
public interface MzSeries extends SeriesValueCount {

  /**
   * @return All mz values corresponding to non-0 intensities.
   */
  DoubleBuffer getMZValues();


  /**
   *
   * @param dst results are reflected in this array
   * @return All m/z values of detected data points.
   */
  double[] getMzValues(double[] dst);

  /**
   * @param index
   * @return The value at the index position. Note the index does not correspond to scan numbers.
   * @see IonTimeSeries#getMzForSpectrum(Scan)
   */
  default double getMZ(int index) {
    return getMZValues().get(index);
  }

  /**
   * @return The number of mz values corresponding to non-0 intensities.
   */
  default int getNumberOfValues() {
    return getMZValues().capacity();
  }

}
