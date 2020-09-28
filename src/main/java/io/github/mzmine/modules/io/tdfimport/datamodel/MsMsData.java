package io.github.mzmine.modules.io.tdfimport.datamodel;

import com.sun.jna.Pointer;

public class MsMsData implements MsMsCallback {

  public long precursorId = 0;
  public int numPeaks = 0;
  public double[] mz_values;
  public float[] intensity_values;

  @Override
  public void invoke(long precursor_id, int num_peaks, Pointer pMz, Pointer pIntensites) {
    precursorId = precursor_id;
    numPeaks = num_peaks;
    mz_values = pMz.getDoubleArray(0, num_peaks);
    intensity_values = pIntensites.getFloatArray(0, num_peaks);
  }
}
