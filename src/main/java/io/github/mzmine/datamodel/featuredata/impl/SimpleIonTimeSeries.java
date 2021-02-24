/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.featuredata.impl;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Used to store LC-MS data.
 *
 * @author https://github.com/SteffenHeu
 */
public class SimpleIonTimeSeries implements IonTimeSeries<Scan> {

  private static final Logger logger = Logger.getLogger(SimpleIonTimeSeries.class.getName());

  protected final List<Scan> scans;
  protected final DoubleBuffer intensityValues;
  protected final DoubleBuffer mzValues;

  /**
   * @param storage         may be null if forceStoreInRam is true
   * @param mzValues
   * @param intensityValues
   * @param scans
   */
  public SimpleIonTimeSeries(@Nullable MemoryMapStorage storage, @Nonnull double[] mzValues,
      @Nonnull double[] intensityValues, @Nonnull List<Scan> scans) {
    if (mzValues.length != intensityValues.length || mzValues.length != scans.size()) {
      throw new IllegalArgumentException("Length of mz, intensity and/or scans does not match.");
    }

    this.scans = scans;

    DoubleBuffer tempMzs;
    DoubleBuffer tempIntensities;
    if (storage != null) {
      try {
        tempMzs = storage.storeData(mzValues);
        tempIntensities = storage.storeData(intensityValues);
      } catch (IOException e) {
        e.printStackTrace();
        tempMzs = DoubleBuffer.wrap(mzValues);
        tempIntensities = DoubleBuffer.wrap(intensityValues);
      }
    } else {
      tempMzs = DoubleBuffer.wrap(mzValues);
      tempIntensities = DoubleBuffer.wrap(intensityValues);
    }

    this.mzValues = tempMzs;
    this.intensityValues = tempIntensities;
  }

  @Override
  public SimpleIonTimeSeries subSeries(@Nullable MemoryMapStorage storage,
      @Nonnull List<Scan> subset) {
    double[] mzs = new double[subset.size()];
    double[] intensities = new double[subset.size()];

    for (int i = 0; i < subset.size(); i++) {
      mzs[i] = getMzForSpectrum(subset.get(i));
      intensities[i] = getIntensityForSpectrum(subset.get(i));
    }

    return new SimpleIonTimeSeries(storage, mzs, intensities, subset);
  }

  @Override
  public double[] getMzValues(double[] dst) {
    if (dst.length < getNumberOfValues()) {
      dst = new double[getNumberOfValues()];
    }
    getMZValues().get(0, dst);
    return dst;
  }

  @Override
  public double[] getIntensityValues(double[] dst) {
    if (dst.length < getNumberOfValues()) {
      dst = new double[getNumberOfValues()];
    }
    getIntensityValues().get(0, dst);
    return dst;
  }

  @Override
  public DoubleBuffer getIntensityValues() {
    return intensityValues;
  }

  @Override
  public DoubleBuffer getMZValues() {
    return mzValues;
  }

  @Override
  public List<Scan> getSpectra() {
    return Collections.unmodifiableList(scans);
  }

  @Override
  public float getRetentionTime(int index) {
    return scans.get(index).getRetentionTime();
  }

  @Override
  public IonSpectrumSeries<Scan> copy(MemoryMapStorage storage) {
    double[][] data = DataPointUtils
        .getDataPointsAsDoubleArray(getMZValues(), getIntensityValues());

    return copyAndReplace(storage, data[0], data[1]);
  }

  @Override
  public IonTimeSeries<Scan> copyAndReplace(@Nullable MemoryMapStorage storage,
      @Nonnull double[] newMzValues,
      @Nonnull double[] newIntensityValues) {

    return new SimpleIonTimeSeries(storage, newMzValues, newIntensityValues, this.scans);
  }
}
