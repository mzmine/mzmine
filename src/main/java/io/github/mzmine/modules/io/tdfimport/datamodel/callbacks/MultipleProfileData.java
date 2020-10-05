package io.github.mzmine.modules.io.tdfimport.datamodel.callbacks;

import com.sun.jna.Pointer;
import java.util.HashMap;
import java.util.Map;

public class MultipleProfileData implements ProfileCallback {

  Map<Long, ProfileDataPoints> spectra = new HashMap<>();

  public class ProfileDataPoints {
    long precursorId;
    long num_points;
    float[] intensities;
    Pointer userData;
  }

  @Override
  public void invoke(long id, long num_points, Pointer pIntensites, Pointer userData) {
    ProfileDataPoints msms_spectrum = new ProfileDataPoints();
    msms_spectrum.precursorId = id;
    msms_spectrum.num_points = num_points;
    msms_spectrum.intensities = pIntensites.getFloatArray(0, (int) num_points);
    spectra.put(id, msms_spectrum);
  }
}
