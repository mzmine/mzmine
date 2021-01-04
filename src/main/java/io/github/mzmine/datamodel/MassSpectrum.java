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
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;

/**
 * This class represent one mass spectrum.
 */
public interface MassSpectrum extends Iterable<DataPoint> {

  /**
   * Returns the m/z range of this spectrum. Never returns null.
   *
   * @return m/z range of this Scan
   */
  @Nonnull
  Range<Double> getDataPointMZRange();

  /**
   * Returns the index of the top intensity data point. May return -1 if there are no data points in
   * this Scan.
   *
   * @return Base peak index
   */
  @Nullable
  Integer getBasePeakIndex();

  /**
   * Returns the sum of intensities of all data points.
   *
   * @return Total ion current
   */
  @Nonnull
  Double getTIC();

  /**
   * Centroid / profile / thresholded
   *
   * @return
   */
  MassSpectrumType getSpectrumType();

  /**
   * @return Number of m/z and intensity data points
   */
  int getNumberOfDataPoints();

  /**
   * Returns data points of this m/z table sorted in m/z order.
   *
   * This method may need to read data from disk, therefore it may be quite slow. Modules should be
   * aware of that and cache the data points if necessary.
   *
   * @return Data points (m/z and intensity pairs) of this scan
   */
  @Nonnull
  DoubleBuffer getMzValues();

  @Nonnull
  DoubleBuffer getIntensityValues();

  double getMzValue(int index);

  double getIntensityValue(int index);

  @Nullable
  Double getBasePeakMz();

  @Nullable
  Double getBasePeakIntensity();

  Stream<DataPoint> stream();



  // @Deprecated DataPoint[] getDataPoints();

  // DataPoint getHighestDataPoint();

  /**
   * @return Returns scan datapoints within a given range
   */
  @Deprecated
  @Nonnull
  DataPoint[] getDataPointsByMass(@Nonnull Range<Double> mzRange);


  /**
   * @return Returns scan datapoints over certain intensity
   */
  @Deprecated
  @Nonnull
  DataPoint[] getDataPointsOverIntensity(double intensity);
}
