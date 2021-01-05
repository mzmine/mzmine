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

import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.MemoryMapStorage;

/**
 * Simple implementation of the Scan interface.
 */
public abstract class AbstractStorableSpectrum implements MassSpectrum {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  protected final MemoryMapStorage storage;
  protected DoubleBuffer mzValues, intensityValues;
  protected Range<Double> mzRange;
  protected int basePeak = -1;
  protected double totalIonCurrent;

  /**
   * Constructor for creating scan with given data
   */
  public AbstractStorableSpectrum(@Nonnull MemoryMapStorage storage) {

    Preconditions.checkNotNull(storage);

    // save scan data
    this.storage = storage;

  }

  public void setDataPoints(DataPoint dataPoints[]) {
    double mzValues[] = new double[dataPoints.length];
    double intensityValues[] = new double[dataPoints.length];
    for (int i = 0; i < dataPoints.length; i++) {
      mzValues[i] = dataPoints[i].getMZ();
      intensityValues[i] = dataPoints[i].getIntensity();
    }
    setDataPoints(mzValues, intensityValues);
  }

  /**
   * @param dataPoints
   */
  public synchronized void setDataPoints(double mzValues[], double intensityValues[]) {

    assert mzValues.length == intensityValues.length;

    for (int i = 0; i < mzValues.length - 1; i++) {
      if (mzValues[i] > mzValues[i + 1]) {
        throw new IllegalArgumentException("The m/z values must be sorted in ascending order");
      }
    }

    try {
      this.mzValues = storage.storeData(mzValues);
      this.intensityValues = storage.storeData(intensityValues);
    } catch (IOException e) {
      e.printStackTrace();
      logger.log(Level.SEVERE,
          "Error while storing data points on disk, keeping them in memory instead", e);
      this.mzValues = DoubleBuffer.wrap(mzValues);
      this.intensityValues = DoubleBuffer.wrap(intensityValues);
    }


    totalIonCurrent = 0;

    // find m/z range and base peak
    if (intensityValues.length > 0) {

      basePeak = 0;
      mzRange = Range.closed(mzValues[0], mzValues[mzValues.length - 1]);

      for (int i = 0; i < intensityValues.length; i++) {

        if (intensityValues[i] > intensityValues[basePeak]) {
          basePeak = i;
        }

        totalIonCurrent += intensityValues[i];

      }

    } else {
      mzRange = Range.singleton(0.0);
      basePeak = -1;
    }

  }

  /**
   * @see io.github.mzmine.datamodel.Scan#getNumberOfDataPoints()
   */
  @Override
  public int getNumberOfDataPoints() {
    return mzValues.capacity();
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
    return basePeak;
  }



  @Override
  public @Nonnull Double getTIC() {
    return totalIonCurrent;
  }



  @Override
  public DoubleBuffer getMzValues() {
    return mzValues;
  }

  @Override
  public DoubleBuffer getIntensityValues() {
    return intensityValues;
  }

  private DataPoint[] getDataPoints() {
    DataPoint d[] = new DataPoint[getNumberOfDataPoints()];
    for (int i = 0; i < getNumberOfDataPoints(); i++) {
      d[i] = new SimpleDataPoint(mzValues.get(i), intensityValues.get(i));
    }
    return d;
  }

  /*
   * @Override public DataPoint getHighestDataPoint() { if (basePeak < 0) return null; double
   * basePeakMz = mzValues.get(basePeak); double basePeakInt = intensityValues.get(basePeak);
   * DataPoint d = new SimpleDataPoint(basePeakMz, basePeakInt); return d; }
   */

  /**
   * @return Returns scan datapoints within a given range
   * @Override
   * @Nonnull public DataPoint[] getDataPointsByMass(@Nonnull Range<Double> mzRange) {
   * 
   *          DataPoint[] dataPoints = getDataPoints(); int startIndex, endIndex; for (startIndex =
   *          0; startIndex < dataPoints.length; startIndex++) { if (dataPoints[startIndex].getMZ()
   *          >= mzRange.lowerEndpoint()) { break; } }
   * 
   *          for (endIndex = startIndex; endIndex < dataPoints.length; endIndex++) { if
   *          (dataPoints[endIndex].getMZ() > mzRange.upperEndpoint()) { break; } }
   * 
   *          DataPoint pointsWithinRange[] = new DataPoint[endIndex - startIndex];
   * 
   *          // Copy the relevant points System.arraycopy(dataPoints, startIndex,
   *          pointsWithinRange, 0, endIndex - startIndex);
   * 
   *          return pointsWithinRange; }
   * 
   */

  @Override
  public double getMzValue(int index) {
    return mzValues.get(index);
  }

  @Override
  public double getIntensityValue(int index) {
    return intensityValues.get(index);
  }

  @Override
  @Nullable
  public Double getBasePeakMz() {
    if (basePeak < 0)
      return null;
    else
      return mzValues.get(basePeak);
  }

  @Override
  @Nullable
  public Double getBasePeakIntensity() {
    if (basePeak < 0)
      return null;
    else
      return intensityValues.get(basePeak);
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

