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

import com.google.common.collect.Range;
import java.nio.DoubleBuffer;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class represent one mass spectrum. Typically the implementation will store the m/z and
 * intensity values on disk using a MemoryMapStorage (see AbstractStorableSpectrum). This class is
 * Iterable, but there is an important point - to avoid consuming memory for each DataPoint
 * instance, we will iterate over the stored data points with a single DataPoint instance that is
 * incrementing an internal cursor. That means this code will work:
 *
 * {@code for (DataPoint d : spectrum) System.out.println(d.getMz() + ":" + d.getIntensity();} but
 * this code will NOT work:
 * {@code ArrayList<DataPoint> list = new ArrayList<>(); list.addAll(spectrum);}
 *
 */
public interface MassSpectrum extends Iterable<DataPoint> {

  /**
   * @return Number of m/z and intensity data points. This corresponds to the capacity of the
   *         DoubleBuffers returned by getMzValues() and getIntensityValues()
   */
  int getNumberOfDataPoints();

  /**
   * Centroid / profile / thresholded
   *
   * @return Spectrum type
   */
  MassSpectrumType getSpectrumType();

  /**
   * @return The m/z values of this spectrum sorted in m/z order. The capacity of the returned
   *         buffer is equivalent to the number of data points in this spectrum.
   */
  @Nonnull
  DoubleBuffer getMzValues();

  /**
   * @return The intensity values corresponding to the m/z values returned by getMzValues(). The
   * capacity of the returned buffer is equivalent to the number of data points in this spectrum.
   */
  @Nonnull
  DoubleBuffer getIntensityValues();

  double[] getMzValues(@Nonnull double[] dst);

  double[] getIntensityValues(@Nonnull double[] dst);

  /**
   * A shortcut method for {@code getMzValues().get(index)}
   */
  double getMzValue(int index);

  /**
   * A shortcut method for {@code getIntensityValues().get(index)}
   */
  double getIntensityValue(int index);

  /**
   * @return The m/z value of the highest data point of this spectrum or null if the spectrum has 0
   *         data points.
   */
  @Nullable
  Double getBasePeakMz();

  /**
   * @return The intensity value of the highest data point of this spectrum or null if the spectrum
   *         has 0 data points.
   */
  @Nullable
  Double getBasePeakIntensity();

  /**
   * @return The index of the top intensity data point or null if the spectrum has 0 data points.
   */
  @Nullable
  Integer getBasePeakIndex();

  /**
   * @return The m/z range of this spectrum or null if the spectrum has 0 data points.
   */
  @Nullable
  Range<Double> getDataPointMZRange();

  /**
   * @return The sum of intensities of all data points or null if the spectrum has 0 data points.
   */
  @Nullable
  Double getTIC();


  /**
   * Creates a stream of DataPoints to iterate over this array. To avoid consuming memory for each
   * DataPoint instance, we will iterate over the stored data points with a single DataPoint
   * instance that is incrementing an internal cursor. That means this code will NOT work:
   *
   * {@code ArrayList<DataPoint> list = spectrum.stream().collect();}
   *
   * @return A stream of DataPoint represented by a single DataPoint instance that is iterating over
   *         the spectrum.
   */
  Stream<DataPoint> stream();

}
