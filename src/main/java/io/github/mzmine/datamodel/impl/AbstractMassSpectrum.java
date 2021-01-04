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

package io.github.mzmine.datamodel.impl;

import java.nio.DoubleBuffer;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.scans.ScanUtils;

/**
 * Simple implementation of the Scan interface.
 */
public abstract class AbstractMassSpectrum implements MassSpectrum {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  protected @Nullable Range<Double> mzRange;
  protected @Nullable Integer basePeakIndex = null;
  protected double totalIonCurrent = 0.0;

  protected synchronized void updateCacheValues(@Nonnull DoubleBuffer mzValues,
      @Nonnull DoubleBuffer intensityValues) {

    assert mzValues != null;
    assert intensityValues != null;
    assert mzValues.capacity() == intensityValues.capacity();

    totalIonCurrent = 0.0;

    if (mzValues.capacity() == 0) {
      mzRange = null;
      basePeakIndex = null;
      return;
    }

    totalIonCurrent = 0.0;
    basePeakIndex = 0;
    mzRange = Range.closed(mzValues.get(0), mzValues.get(mzValues.capacity() - 1));

    for (int i = 0; i < mzValues.capacity() - 1; i++) {

      // Check the order of the m/z values
      if ((i < mzValues.capacity() - 1) && (mzValues.get(i) > mzValues.get(i + 1))) {
        throw new IllegalArgumentException("The m/z values must be sorted in ascending order");
      }

      // Update base peak index
      if (intensityValues.get(i) > intensityValues.get(basePeakIndex)) {
        basePeakIndex = i;
      }

      // Update TIC
      totalIonCurrent += intensityValues.get(i);

    }

  }


  /**
   * @see io.github.mzmine.datamodel.Scan#getNumberOfDataPoints()
   */
  @Override
  public int getNumberOfDataPoints() {
    return getMzValues().capacity();
  }

  /**
   * @see io.github.mzmine.datamodel.Scan#
   */
  @Override
  @Nonnull
  public Range<Double> getDataPointMZRange() {
    return mzRange;
  }

  /**
   * @see Scan#getHighestDataPoint()
   */
  @Override
  public @Nullable Integer getBasePeakIndex() {
    return basePeakIndex;
  }



  @Override
  public @Nonnull Double getTIC() {
    return totalIonCurrent;
  }

  /*
   * @Override public DataPoint getHighestDataPoint() { if (basePeak < 0) return null; double
   * basePeakMz = mzValues.get(basePeak); double basePeakInt = intensityValues.get(basePeak);
   * DataPoint d = new SimpleDataPoint(basePeakMz, basePeakInt); return d; }
   */

  /**
   * @return Returns scan datapoints within a given range
   */
  @Override
  @Nonnull
  public DataPoint[] getDataPointsByMass(@Nonnull Range<Double> mzRange) {

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

  @Deprecated
  private DataPoint[] getDataPoints() {
    return ScanUtils.extractDataPoints(this);
  }

  /**
   * @return Returns scan datapoints over certain intensity
   */
  @Override
  @Nonnull
  public DataPoint[] getDataPointsOverIntensity(double intensity) {
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

  @Override
  public double getMzValue(int index) {
    return getMzValues().get(index);
  }

  @Override
  public double getIntensityValue(int index) {
    return getIntensityValues().get(index);
  }

  @Override
  @Nullable
  public Double getBasePeakMz() {
    if (basePeakIndex == null)
      return null;
    else
      return getMzValues().get(basePeakIndex);
  }

  @Override
  @Nullable
  public Double getBasePeakIntensity() {
    if (basePeakIndex == null)
      return null;
    else
      return getIntensityValues().get(basePeakIndex);
  }

  @Override
  public Iterator<DataPoint> iterator() {
    return new DataPointIterator(this);
  }

  @Override
  public Stream<DataPoint> stream() {
    return Streams.stream(this);
  }

  private class DataPointIterator implements Iterator<DataPoint>, DataPoint {

    // We start at -1 so the first call to next() moves us to index 0
    private int cursor = -1;
    private final MassSpectrum spectrum;

    DataPointIterator(MassSpectrum spectrum) {
      this.spectrum = spectrum;
    }

    @Override
    public boolean hasNext() {
      return (cursor + 1) < spectrum.getNumberOfDataPoints();
    }

    @Override
    public DataPoint next() {
      cursor++;
      return this;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public double getMZ() {
      return spectrum.getMzValue(cursor);
    }

    @Override
    public double getIntensity() {
      return spectrum.getIntensityValue(cursor);
    }

  }
}

