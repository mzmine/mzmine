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
public class StorableIonMobilitySeries implements IonMobilitySeries, ModifiableSpectra<MobilityScan> {

  private static final Logger logger = Logger.getLogger(StorableIonMobilitySeries.class.getName());

  protected final List<MobilityScan> scans;

  protected final int storageOffset;
  protected final int numValues;
  protected final SimpleIonMobilogramTimeSeries ionTrace;

  protected StorableIonMobilitySeries(final SimpleIonMobilogramTimeSeries ionTrace,
      final int offset,
      final int numValues, @Nonnull List<MobilityScan> scans) {
    if (numValues != scans.size()) {
      throw new IllegalArgumentException("numPoints and number of scans scans does not match.");
    }

    final Frame frame = scans.get(0).getFrame();
    for (MobilityScan scan : scans) {
      if (frame != scan.getFrame()) {
        throw new IllegalArgumentException("All mobility scans must belong to the same frame.");
      }
    }

    this.storageOffset = offset;
    this.numValues = numValues;
    this.scans = scans;
    this.ionTrace = ionTrace;
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
  public IonMobilitySeries subSeries(@Nullable MemoryMapStorage storage,
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
  public double getIntensity(int index) {
    return ionTrace.getMobilogramIntensityValue(this, index);
  }

  @Override
  public double getMZ(int index) {
    return ionTrace.getMobilogramMzValue(this, index);
  }

  @Override
  public DoubleBuffer getIntensityValues() {
    return ionTrace.getMobilogramIntensityValues(this);
  }

  @Override
  public DoubleBuffer getMZValues() {
    return ionTrace.getMobilogramMzValues(this);
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

  public int getStorageOffset() {
    return storageOffset;
  }

  @Override
  public int getNumberOfValues() {
    return numValues;
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
