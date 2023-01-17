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
