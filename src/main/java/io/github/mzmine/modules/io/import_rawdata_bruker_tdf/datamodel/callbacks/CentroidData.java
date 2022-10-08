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
