package io.github.mzmine.modules.io.tdfimport.datamodel.callbacks;

import com.sun.jna.Pointer;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;

public class ProfileData implements ProfileCallback {

  long id;
  long num_points;
  int[] intensities;
  Pointer userData;

  @Override
  public void invoke(long id, long num_points, Pointer intensity_values, Pointer userData) {
    this.id = id;
    this.num_points = num_points;
    this.intensities = intensity_values.getIntArray(0, (int) num_points);
    this.userData = userData;
  }

  public DataPoint[] toDataPoints(double lowMass, double highMass) {
    assert highMass > lowMass;
    double range = highMass - lowMass;
    double step = range/num_points;

    DataPoint[] dps = new DataPoint[(int) num_points];

    for(int i = 0; i < num_points; i++) {
      dps[i] = new SimpleDataPoint(lowMass + step * i, intensities[i]);
    }
    return dps;
  }
}