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

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.callbacks;

import com.sun.jna.Pointer;
import io.github.mzmine.modules.io.import_rawdata_mzml.ConversionUtils;

public class CentroidData implements CentroidCallback {

  private long precursorId;
  private int numPeaks;
  private double[] mzs;
  private float[] intensities;

  @Override
  public void invoke(long precursor_id, int num_peaks, Pointer pMz, Pointer pIntensites,
      Pointer userData) {
    precursorId = precursor_id;
    numPeaks = num_peaks;
    if(num_peaks != 0) {
      mzs = pMz.getDoubleArray(0, num_peaks);
      intensities = pIntensites.getFloatArray(0, num_peaks);
    } else {
      mzs = new double[0];
      intensities = new float[0];
    }
  }

  /**
   * @return [0][] mzs, [1][] intensities
   */
  public final double[][] toDoubles() {
    double[][] data = new double[2][];
    data[0] = mzs;
    data[1] = ConversionUtils.convertFloatsToDoubles(intensities);
    return data;
  }
}
