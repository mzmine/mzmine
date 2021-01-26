/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.import_bruker_tdf.datamodel.callbacks;

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