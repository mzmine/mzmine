package io.github.mzmine.modules.io.tdfimport.datamodel.callbacks;

import com.sun.jna.Pointer;

public class ProfileMsMsData implements ProfileMsMsCallback{

  long id;
  long num_points;
  long[] intensities;
  Pointer userData;

  @Override
  public void invoke(long id, long num_points, Pointer intensity_values, Pointer userData) {
    this.id = id;
    this.num_points = num_points;
    intensities = intensity_values.getLongArray(0, (int) num_points);
    this.userData = userData;
  }
}
