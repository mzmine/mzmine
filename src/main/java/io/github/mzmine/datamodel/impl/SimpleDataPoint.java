/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel.impl;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.main.MZmineCore;
import java.text.Format;
import java.util.Objects;

/**
 * This class represents one data point of a spectrum (m/z and intensity pair). Data point is
 * immutable once created, to make things simple.
 */
public class SimpleDataPoint implements DataPoint {

  private final double mz;
  private final double intensity;

  /**
   * Constructor which copies the data from another DataPoint
   */
  public SimpleDataPoint(DataPoint dp) {
    this.mz = dp.getMZ();
    this.intensity = dp.getIntensity();
  }

  /**
   * @param mz
   * @param intensity
   */
  public SimpleDataPoint(double mz, double intensity) {
    this.mz = mz;
    this.intensity = intensity;
  }

  @Override
  public double getIntensity() {
    return intensity;
  }

  @Override
  public double getMZ() {
    return mz;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SimpleDataPoint that = (SimpleDataPoint) o;
    return Double.compare(that.mz, mz) == 0 && Double.compare(that.intensity, intensity) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mz, intensity);
  }

  @Override
  public String toString() {
    Format mzFormat = MZmineCore.getConfiguration().getMZFormat();
    Format intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    String str =
        "m/z: " + mzFormat.format(mz) + ", intensity: " + intensityFormat.format(intensity);
    return str;
  }

}
