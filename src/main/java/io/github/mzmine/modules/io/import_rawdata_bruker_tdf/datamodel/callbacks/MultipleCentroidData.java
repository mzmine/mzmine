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
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import java.util.HashMap;
import java.util.Map;

public class MultipleCentroidData implements CentroidCallback {

  public class CentroidDataPoints {
    public long precursorId = 0;
    public double[] mzs;
    public float[] intensities;

    /**
     * Creates an array of data points. Dont call before
     * @return
     */
    public final DataPoint[] toDataPoints() {
      DataPoint[] dps = new DataPoint[mzs.length];

      for(int i = 0; i < mzs.length; i++) {
        dps[i] = new SimpleDataPoint(mzs[i], intensities[i]);
      }
      return dps;
    }
  }
  Map<Long, CentroidDataPoints> msmsSpectra = new HashMap<>();

  @Override
  public void invoke(long precursor_id, int num_peaks, Pointer pMz, Pointer pIntensites,
      Pointer userData) {
    CentroidDataPoints msms_spectrum = new CentroidDataPoints();
    msms_spectrum.precursorId = precursor_id;
    msms_spectrum.mzs = pMz.getDoubleArray(0, num_peaks);
    msms_spectrum.intensities = pIntensites.getFloatArray(0, num_peaks);
    msmsSpectra.put(precursor_id, msms_spectrum);
  }
}
