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

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.nio.DoubleBuffer;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Stores data points of several {@link MobilityScan}s. Usually wrapped in a {@link
 * SimpleIonMobilogramTimeSeries} representing the same feature with mobility resolution.
 *
 * @author https://github.com/SteffenHeu
 */
public class SimpleIonMobilitySeries implements IonMobilitySeries, ModifiableSpectra<MobilityScan> {

  private static final Logger logger = Logger.getLogger(SimpleIonMobilitySeries.class.getName());

  protected final List<MobilityScan> scans;

  protected final DoubleBuffer intensityValues;
  protected final DoubleBuffer mzValues;

  /**
   * @param storage         May be null if forceStoreInRam is true.
   * @param mzValues
   * @param intensityValues
   * @param scans
   */
  public SimpleIonMobilitySeries(@Nullable MemoryMapStorage storage, @Nonnull double[] mzValues,
      @Nonnull double[] intensityValues, @Nonnull List<MobilityScan> scans) {
    if (mzValues.length != intensityValues.length || mzValues.length != scans.size()) {
      throw new IllegalArgumentException("Length of mz, intensity and/or scans does not match.");
    }

    final Frame frame = scans.get(0).getFrame();
    for (MobilityScan scan : scans) {
      if (frame != scan.getFrame()) {
        throw new IllegalArgumentException("All mobility scans must belong to the same frame.");
      }
    }

    this.scans = scans;
    this.mzValues = StorageUtils.storeValuesToDoubleBuffer(storage, mzValues);
    this.intensityValues = StorageUtils.storeValuesToDoubleBuffer(storage, intensityValues);
  }

  @Override
  public double getIntensityForSpectrum(MobilityScan spectrum) {
    int index = scans.indexOf(spectrum);
    if (index != -1) {
      return getIntensity(index);
    }
    return 0d;
  }

  @Override
  public double getMzForSpectrum(MobilityScan spectrum) {
    int index = scans.indexOf(spectrum);
    if (index != -1) {
      return getMZ(index);
    }
    return 0d;
  }

  @Override
  public IonSpectrumSeries<MobilityScan> subSeries(@Nullable MemoryMapStorage storage,
      @Nonnull List<MobilityScan> subset) {
    double[] mzs = new double[subset.size()];
    double[] intensities = new double[subset.size()];

    for (int i = 0; i < subset.size(); i++) {
      mzs[i] = getMzForSpectrum(subset.get(i));
      intensities[i] = getIntensityForSpectrum(subset.get(i));
    }

    return new SimpleIonMobilitySeries(storage, mzs, intensities, subset);
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

  public double getMobility(int index) {
    return getSpectra().get(index).getMobility();
  }

  @Override
  public List<MobilityScan> getSpectra() {
    return Collections.unmodifiableList(scans);
  }

  @Override
  public IonSpectrumSeries<MobilityScan> copy(@Nullable MemoryMapStorage storage) {
    double[][] data = DataPointUtils
        .getDataPointsAsDoubleArray(getMZValues(), getIntensityValues());

    return new SimpleIonMobilitySeries(storage, data[0], data[1], scans);
  }

  @Override
  public List<MobilityScan> getSpectraModifiable() {
    return scans;
  }

  @Override
  public IonSpectrumSeries<MobilityScan> copyAndReplace(@Nullable MemoryMapStorage storage,
      @Nonnull double[] newMzValues, @Nonnull double[] newIntensityValues) {
    return new SimpleIonMobilitySeries(storage, newMzValues, newIntensityValues, scans);
  }
}
