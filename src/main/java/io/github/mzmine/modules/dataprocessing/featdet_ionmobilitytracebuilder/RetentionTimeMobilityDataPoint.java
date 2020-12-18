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

package io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder;

import io.github.mzmine.datamodel.DataPoint;

public class RetentionTimeMobilityDataPoint implements DataPoint {

  private final double mobility;
  private final double mz;
  private final Float retentionTime;
  private final double intensity;
  private final int frameNumber;
  private final int scanNumber;
  private final double dataPointWidth;
  private final double dataPointHeight;

  public RetentionTimeMobilityDataPoint(double mobility, double mz, Float retentionTime,
      double intensity, int frameNumber, int scanNumber, double dataPointWidth,
      double dataPointHeight) {
    this.mobility = mobility;
    this.mz = mz;
    this.retentionTime = retentionTime;
    this.intensity = intensity;
    this.frameNumber = frameNumber;
    this.scanNumber = scanNumber;
    this.dataPointWidth = dataPointWidth;
    this.dataPointHeight = dataPointHeight;
  }

  public double getMobility() {
    return mobility;
  }

  public double getMZ() {
    return mz;
  }

  public Float getRetentionTime() {
    return retentionTime;
  }

  public double getIntensity() {
    return intensity;
  }

  public int getFrameNumber() {
    return frameNumber;
  }

  public int getScanNumber() {
    return scanNumber;
  }

  public double getDataPointWidth() {
    return dataPointWidth;
  }

  public double getDataPointHeight() {
    return dataPointHeight;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(intensity);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(mobility);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(mz);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + ((retentionTime == null) ? 0 : retentionTime.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    RetentionTimeMobilityDataPoint other = (RetentionTimeMobilityDataPoint) obj;
    if (Double.doubleToLongBits(intensity) != Double.doubleToLongBits(other.intensity))
      return false;
    if (Double.doubleToLongBits(mobility) != Double.doubleToLongBits(other.mobility))
      return false;
    if (Double.doubleToLongBits(mz) != Double.doubleToLongBits(other.mz))
      return false;
    if (retentionTime == null) {
      if (other.retentionTime != null)
        return false;
    } else if (!retentionTime.equals(other.retentionTime))
      return false;
    return true;
  }

}
