package io.github.mzmine.modules.io.tdfimport.datamodel.callbacks;

import com.sun.jna.Pointer;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;

public class MsMsDataV2 implements MsMsCallbackV2 {

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
