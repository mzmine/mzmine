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

package io.github.mzmine.datamodel;

import java.nio.DoubleBuffer;
import java.util.Vector;
import javax.annotation.Nonnull;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;

/**
 * This class represent one mass spectrum.
 */
public interface MassSpectrum {

  /**
   * Returns the m/z range of this spectrum. Never returns null.
   *
   * @return m/z range of this Scan
   */
  @Nonnull
  public Range<Double> getDataPointMZRange();

  /**
   * Returns the index of the top intensity data point. May return -1 if there are no data points in
   * this Scan.
   *
   * @return Base peak index
   */
  public int getBasePeak();

  /**
   * Returns the sum of intensities of all data points.
   *
   * @return Total ion current
   */
  public double getTIC();

  /**
   * Centroid / profile / thresholded
   *
   * @return
   */
  public MassSpectrumType getSpectrumType();

  /**
   * @return Number of m/z and intensity data points
   */
  public int getNumberOfDataPoints();

  /**
   * Returns data points of this m/z table sorted in m/z order.
   *
   * This method may need to read data from disk, therefore it may be quite slow. Modules should be
   * aware of that and cache the data points if necessary.
   *
   * @return Data points (m/z and intensity pairs) of this scan
   */
  @Nonnull
  public DoubleBuffer getMzValues();

  @Nonnull
  public DoubleBuffer getIntensityValues();

  default DataPoint[] getDataPoints() {
    DataPoint d[] = new DataPoint[getNumberOfDataPoints()];
    for (int i = 0; i < getNumberOfDataPoints(); i++) {
      d[i] = new SimpleDataPoint(getMzValues().get(i), getIntensityValues().get(i));
    }
    return d;
  }

  default DataPoint getHighestDataPoint() {
    DataPoint d = new SimpleDataPoint(getMzValues().get(getBasePeak()),
        getIntensityValues().get(getBasePeak()));
    return d;
  }

  /**
   * @return Returns scan datapoints within a given range
   */
  @Nonnull
  default DataPoint[] getDataPointsByMass(@Nonnull Range<Double> mzRange) {

    DataPoint[] dataPoints = getDataPoints();
    int startIndex, endIndex;
    for (startIndex = 0; startIndex < dataPoints.length; startIndex++) {
      if (dataPoints[startIndex].getMZ() >= mzRange.lowerEndpoint()) {
        break;
      }
    }

    for (endIndex = startIndex; endIndex < dataPoints.length; endIndex++) {
      if (dataPoints[endIndex].getMZ() > mzRange.upperEndpoint()) {
        break;
      }
    }

    DataPoint pointsWithinRange[] = new DataPoint[endIndex - startIndex];

    // Copy the relevant points
    System.arraycopy(dataPoints, startIndex, pointsWithinRange, 0, endIndex - startIndex);

    return pointsWithinRange;
  }

  /**
   * @return Returns scan datapoints over certain intensity
   */
  @Nonnull
  default DataPoint[] getDataPointsOverIntensity(double intensity) {
    int index;
    Vector<DataPoint> points = new Vector<DataPoint>();
    DataPoint[] dataPoints = getDataPoints();
    for (index = 0; index < dataPoints.length; index++) {
      if (dataPoints[index].getIntensity() >= intensity) {
        points.add(dataPoints[index]);
      }
    }

    DataPoint pointsOverIntensity[] = points.toArray(new DataPoint[0]);

    return pointsOverIntensity;
  }

}
