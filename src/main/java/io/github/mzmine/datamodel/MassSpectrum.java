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

package io.github.mzmine.datamodel;

import com.google.common.collect.Range;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class represent one mass spectrum. Typically the implementation will store the m/z and
 * intensity values on disk using a MemoryMapStorage (see AbstractStorableSpectrum). This class is
 * Iterable, but there is an important point - to avoid consuming memory for each DataPoint
 * instance, we will iterate over the stored data points with a single DataPoint instance that is
 * incrementing an internal cursor. That means this code will work:
 * <p>
 * {@code for (DataPoint d : spectrum) System.out.println(d.getMz() + ":" + d.getIntensity();} but
 * this code will NOT work: {@code ArrayList<DataPoint> list = new ArrayList<>();
 * list.addAll(spectrum);}
 */
public interface MassSpectrum extends Iterable<DataPoint> {

  /**
   * @return Number of m/z and intensity data points. This corresponds to the capacity of the
   * DoubleBuffers returned by getMzValues() and getIntensityValues()
   */
  int getNumberOfDataPoints();

  /**
   * Centroid / profile / thresholded
   *
   * @return Spectrum type
   */
  MassSpectrumType getSpectrumType();

  /**
   * @param dst A buffer the m/z values will be written into. The buffer should ideally have the
   *            size {@link #getNumberOfDataPoints()}. Some implementations of mass spectrum
   *            reallocate a new buffer, if the current buffer is not big enough.
   * @return The buffer the m/z values were written into. Usually the same as the supplied buffer.
   * However, a new buffer will be allocated if the original buffer is not big enough.
   */
  double[] getMzValues(@NotNull double[] dst);

  void getMzValues(double[] dst, int dstStart);


  /**
   * @param dst A buffer the intensity values will be written into. The buffer should ideally have
   *            the size {@link #getNumberOfDataPoints()}. Some implementations of mass spectrum
   *            reallocate a new buffer, if the current buffer is not big enough.
   * @return The buffer the intensity values were written into. Usually the same as the supplied
   * buffer. However, a new buffer will be allocated if the original buffer is not big enough.
   */
  double[] getIntensityValues(@NotNull double[] dst);

  void getIntensityValues(double[] dst, int dstStart);

  /**
   *
   * @param index The data point index.
   * @return The m/z at the given index.
   */
  double getMzValue(int index);

  /**
   *
   * @param index The data point index.
   * @return The intensity at the given index.
   */
  double getIntensityValue(int index);

  /**
   * @return The m/z value of the highest data point of this spectrum or null if the spectrum has 0
   * data points.
   */
  @Nullable
  Double getBasePeakMz();

  /**
   * @return The intensity value of the highest data point of this spectrum or null if the spectrum
   * has 0 data points.
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
   * <p>
   * {@code ArrayList<DataPoint> list = spectrum.stream().collect();}
   *
   * @return A stream of DataPoint represented by a single DataPoint instance that is iterating over
   * the spectrum.
   */
  Stream<DataPoint> stream();

}
