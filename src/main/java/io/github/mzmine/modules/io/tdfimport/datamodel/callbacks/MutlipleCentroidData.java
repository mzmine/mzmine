package io.github.mzmine.modules.io.tdfimport.datamodel.callbacks;

import com.sun.jna.Pointer;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import java.util.HashMap;
import java.util.Map;

public class MutlipleCentroidData implements CentroidCallback {

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
