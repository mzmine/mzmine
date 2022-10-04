/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
