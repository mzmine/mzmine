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
