/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import org.jetbrains.annotations.NotNull;

public class RetentionTimeMobilityDataPoint implements DataPoint, Comparable {

  private final double mz;
  private final double intensity;
  private final MobilityScan mobilityScan;

  public RetentionTimeMobilityDataPoint(MobilityScan mobilityScan, double mz, double intensity) {
    this.mz = mz;
    this.intensity = intensity;
    this.mobilityScan = mobilityScan;
  }

  public double getMobility() {
    return mobilityScan.getMobility();
  }

  public double getMZ() {
    return mz;
  }

  public float getRetentionTime() {
    return mobilityScan.getRetentionTime();
  }

  public double getIntensity() {
    return intensity;
  }

  public Frame getFrame() {
    return mobilityScan.getFrame();
  }

  public MobilityScan getMobilityScan() {
    return mobilityScan;
  }

  @Override
  public int compareTo(@NotNull Object o) {
    if (o instanceof RetentionTimeMobilityDataPoint) {
      int i = Double.compare(getIntensity(), ((RetentionTimeMobilityDataPoint) o).getIntensity());
      if (i != 0) {
        return i * -1; // descending, most intense first
      }
      int f = Integer.compare(getFrame().getFrameId(),
          ((RetentionTimeMobilityDataPoint) o).getFrame().getFrameId());
      if (f != 0) {
        return f;
      }
      int m = Integer.compare(getMobilityScan().getMobilityScanNumber(),
          ((RetentionTimeMobilityDataPoint) o).getMobilityScan().getMobilityScanNumber());
      if(m != 0) {
        return m;
      }
      return Double.compare(getMZ(), ((RetentionTimeMobilityDataPoint) o).getMZ());
    }
    return -1;
  }

  /*@Override
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
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    RetentionTimeMobilityDataPoint other = (RetentionTimeMobilityDataPoint) obj;
    if (Double.doubleToLongBits(intensity) != Double.doubleToLongBits(other.intensity)) {
      return false;
    }
    if (Double.doubleToLongBits(mobility) != Double.doubleToLongBits(other.mobility)) {
      return false;
    }
    if (Double.doubleToLongBits(mz) != Double.doubleToLongBits(other.mz)) {
      return false;
    }
    if (retentionTime == null) {
      if (other.retentionTime != null) {
        return false;
      }
    } else if (!retentionTime.equals(other.retentionTime)) {
      return false;
    }
    return true;
  }*/

}
