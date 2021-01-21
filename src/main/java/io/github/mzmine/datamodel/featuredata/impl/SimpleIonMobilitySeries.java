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
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Stores data points of several {@link MobilityScan}s. Usually wrapped in a {@link
 * SimpleIonMobilogramTimeSeries} representing the same feature with mobility resolution.
 *
 * @author https://github.com/SteffenHeu
 */
public class SimpleIonMobilitySeries implements IonSpectrumSeries<MobilityScan> {

  private static final Logger logger = Logger.getLogger(SimpleIonMobilitySeries.class.getName());

  protected final List<MobilityScan> scans;
  protected final boolean forceStoreInRam;

  protected DoubleBuffer intensityValues;
  protected DoubleBuffer mzValues;

  /**
   * Creates a {@link SimpleIonMobilitySeries}.
   *
   * @param storage
   * @param mzValues
   * @param intensityValues
   * @param scans
   */
  public SimpleIonMobilitySeries(@Nonnull MemoryMapStorage storage, @Nonnull double[] mzValues,
      @Nonnull double[] intensityValues, @Nonnull List<MobilityScan> scans) {
    this(storage, mzValues, intensityValues, scans, false);
  }

  /**
   * @param storage         May be null if forceStoreInRam is true.
   * @param mzValues
   * @param intensityValues
   * @param scans
   * @param forceStoreInRam Forces storage of mz and intensity values in ram. Note that all series
   *                        created as subset or copy from this series will also be stored in ram.
   */
  public SimpleIonMobilitySeries(@Nullable MemoryMapStorage storage, @Nonnull double[] mzValues,
      @Nonnull double[] intensityValues, @Nonnull List<MobilityScan> scans,
      boolean forceStoreInRam) {
    if (mzValues.length != intensityValues.length || mzValues.length != scans.size()) {
      throw new IllegalArgumentException("Length of mz, intensity and/or scans does not match.");
    }
    if (storage == null && !forceStoreInRam) {
      throw new IllegalArgumentException("MemoryMapStorage is null, cannot store data.");
    }

    final Frame frame = scans.get(0).getFrame();
    for (MobilityScan scan : scans) {
      if (frame != scan.getFrame()) {
        throw new IllegalArgumentException("All mobility scans must belong to the same frame.");
      }
    }

    this.scans = scans;
    this.forceStoreInRam = forceStoreInRam;

    if (!forceStoreInRam) {
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
    } else {
      this.mzValues = DoubleBuffer.wrap(mzValues);
      this.intensityValues = DoubleBuffer.wrap(intensityValues);
    }
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
  public IonSpectrumSeries<MobilityScan> subSeries(MemoryMapStorage storage,
      List<MobilityScan> subset) {
    double[] mzs = new double[subset.size()];
    double[] intensities = new double[subset.size()];

    for (int i = 0; i < subset.size(); i++) {
      mzs[i] = getMzForSpectrum(subset.get(i));
      intensities[i] = getIntensityForSpectrum(subset.get(i));
    }

    return new SimpleIonMobilitySeries(storage, mzs, intensities, subset, forceStoreInRam);
  }

  @Override
  public DoubleBuffer getIntensityValues() {
    return intensityValues;
  }

  @Override
  public DoubleBuffer getMZValues() {
    return mzValues;
  }

  public double getMobility(int index) {
    return getSpectra().get(index).getMobility();
  }

  @Override
  public List<MobilityScan> getSpectra() {
    return Collections.unmodifiableList(scans);
  }

  @Override
  public IonSpectrumSeries<MobilityScan> copy(MemoryMapStorage storage) {
    double[][] data = DataPointUtils
        .getDataPointsAsDoubleArray(getMZValues(), getIntensityValues());

    return new SimpleIonMobilitySeries(storage, data[0], data[1], scans, forceStoreInRam);
  }
}
