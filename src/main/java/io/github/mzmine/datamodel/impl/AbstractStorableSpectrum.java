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

import com.google.common.base.Preconditions;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * An implementation of MassSpectrum that stores the data points in a MemoryMapStorage.
 */
public abstract class AbstractStorableSpectrum extends AbstractMassSpectrum {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private static final DoubleBuffer EMPTY_BUFFER = DoubleBuffer.wrap(new double[0]);

  protected final MemoryMapStorage storage;
  protected DoubleBuffer mzValues;
  protected DoubleBuffer intensityValues;

  /**
   * Constructor for creating scan with given data
   */
  public AbstractStorableSpectrum(@Nonnull MemoryMapStorage storage) {
    Preconditions.checkNotNull(storage);
    // save scan data
    this.storage = storage;
  }

  /**
   * Constructor for creating scan with given data
   */
  public AbstractStorableSpectrum(@Nonnull MemoryMapStorage storage, @Nonnull double mzValues[],
      @Nonnull double intensityValues[]) {
    this(storage);
    setDataPoints(mzValues, intensityValues);
  }

  @Deprecated
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
  public synchronized void setDataPoints(@Nonnull double mzValues[],
      @Nonnull double intensityValues[]) {

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

    updateMzRangeAndTICValues();
  }

  DoubleBuffer getMzValues() {
    if (mzValues == null) {
      return EMPTY_BUFFER;
    } else {
      return mzValues;
    }
  }

  DoubleBuffer getIntensityValues() {
    if (intensityValues == null) {
      return EMPTY_BUFFER;
    } else {
      return intensityValues;
    }
  }

  @Override
  public double[] getMzValues(@Nonnull double[] dst) {
    if (dst.length < getNumberOfDataPoints()) {
      dst = new double[getNumberOfDataPoints()];
    }
    mzValues.get(0, dst);
    return dst;
  }

  @Override
  public double[] getIntensityValues(@Nonnull double[] dst) {
    if (dst.length < getNumberOfDataPoints()) {
      dst = new double[getNumberOfDataPoints()];
    }
    intensityValues.get(0, dst);
    return dst;
  }
}

