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

package io.github.mzmine.modules.io.rawdataimport.fileformats.tdfimport.datamodel.callbacks;

import com.sun.jna.Pointer;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;

public class CentroidData implements CentroidCallback {

  private long precursorId;
  private int numPeaks;
  private double[] mzs;
  private float[] intensites;

  @Override
  public void invoke(long precursor_id, int num_peaks, Pointer pMz, Pointer pIntensites,
      Pointer userData) {
    precursorId = precursor_id;
    numPeaks = num_peaks;
    mzs = pMz.getDoubleArray(0, num_peaks);
    intensites = pIntensites.getFloatArray(0, num_peaks);
  }

  /**
   * Creates an array of data points. Dont call before
   * @return
   */
  public final DataPoint[] toDataPoints() {
    DataPoint[] dps = new DataPoint[numPeaks];

    for(int i = 0; i < numPeaks; i++) {
      dps[i] = new SimpleDataPoint(mzs[i], intensites[i]);
    }
    return dps;
  }
}
