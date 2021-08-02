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

package io.github.mzmine.datamodel.featuredata.impl;

import com.google.common.collect.Comparators;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.nio.DoubleBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
  public SimpleIonTimeSeries(@Nullable MemoryMapStorage storage, @NotNull double[] mzValues,
      @NotNull double[] intensityValues, @NotNull List<Scan> scans) {
    if (mzValues.length != intensityValues.length || mzValues.length != scans.size()) {
      throw new IllegalArgumentException("Length of mz, intensity and/or scans does not match.");
    }

    this.scans = scans;

    this.mzValues = StorageUtils.storeValuesToDoubleBuffer(storage, mzValues);
    this.intensityValues = StorageUtils.storeValuesToDoubleBuffer(storage, intensityValues);
  }

  @Override
  public SimpleIonTimeSeries subSeries(@Nullable MemoryMapStorage storage,
      @NotNull List<Scan> subset) {
    double[] mzs = new double[subset.size()];
    double[] intensities = new double[subset.size()];

    final List<Scan> spectra = getSpectra();
    int sindex = 0;
    for (int i = 0; i < subset.size(); i++) {
      Scan sub = subset.get(i);
      // find next spectrum
      while (spectra.get(sindex) != sub) {
        sindex++;
        if (sindex >= spectra.size()) {
          // exceptional case: no scan in spectra that matches sub from the subset, throw
          if (!Comparators.isInOrder(subset, Comparator.comparingInt(Scan::getScanNumber))) {
            throw new IllegalArgumentException(
                "Subset of scans was not sorted by scan number (which should reflect retention time / mobility)");
          }
          if (!Comparators.isInOrder(spectra, Comparator.comparingInt(Scan::getScanNumber))) {
            throw new IllegalArgumentException(
                "Original IonTimeSeries scans were not sorted by scan number (which should reflect retention time / mobility)");
          }
          throw new IllegalArgumentException(
              "Not all scans of subset were present in this IonTimeSeries");
        }
      }
      // set mz
      mzs[i] = getMZ(sindex);
      intensities[i] = getIntensity(sindex);
    }

    return new SimpleIonTimeSeries(storage, mzs, intensities, subset);
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
      @NotNull double[] newMzValues,
      @NotNull double[] newIntensityValues) {

    return new SimpleIonTimeSeries(storage, newMzValues, newIntensityValues, this.scans);
  }
}
