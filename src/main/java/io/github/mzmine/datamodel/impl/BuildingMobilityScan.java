/*
 *  Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * During raw data import, we need to cache the m/z and intensity values of mobility scans, so we
 * can store all m/zs and intensities of a frame in a single double buffer to save memory.
 * Therefore, we only implement the basic functionality here to limit the usage of this class to the
 * loading process.
 */
public class BuildingMobilityScan implements MobilityScan {

  final int scanNumber;
  final double[] intensityValues;
  final double[] mzValues;
  int basePeakIndex;

  /**
   *
   * @param scanNumber The scan number beginning with 0
   * @param mzIntensities The m/z values [0][n] and intensity values [1][n]
   */
  public BuildingMobilityScan(int scanNumber, double[][] mzIntensities) {
    this(scanNumber, mzIntensities[0], mzIntensities[1]);
  }

  /**
   *
   * @param scanNumber The scan number beginning with 0
   * @param mzs The m/z values
   * @param intensities The intensity values.
   */
  public BuildingMobilityScan(int scanNumber, double[] mzs, double[] intensities) {
    assert intensities.length == mzs.length;

    this.scanNumber = scanNumber;
    boolean haveToSort = false;

    // -1 is intended to be used in mobility scans. The MobilityScan will return null,
    // it this value is -1
    basePeakIndex = -1;
    if (mzs.length > 1) {
      basePeakIndex = 0;
      for (int i = 1; i < mzs.length; i++) {
        if (intensities[i] > intensities[basePeakIndex]) {
          basePeakIndex = i;
        }
        if (mzs[i - 1] > mzs[i]) {
          haveToSort = true;
          break;
        }
      }
    } else if (mzs.length == 1) {
      basePeakIndex = 0;
    }

    if (!haveToSort) {
      this.intensityValues = intensities;
      this.mzValues = mzs;
      return;
    }

    DataPoint[] dps = new DataPoint[mzs.length];
    for (int i = 0; i < mzs.length; i++) {
      dps[i] = new SimpleDataPoint(mzs[i], intensities[i]);
    }
    DataPointSorter sorter = new DataPointSorter(SortingProperty.MZ, SortingDirection.Ascending);
    Arrays.sort(dps, sorter);
    double[][] data = DataPointUtils.getDataPointsAsDoubleArray(dps);

    mzs = data[0];
    intensities = data[1];

    basePeakIndex = 0;
    for (int i = 1; i < mzs.length; i++) {
      if (intensities[i] > intensities[basePeakIndex]) {
        basePeakIndex = i;
      }
    }
    this.intensityValues = intensities;
    this.mzValues = mzs;
  }

  public double[] getMzValues() {
    return mzValues;
  }

  public double[] getIntensityValues() {
    return intensityValues;
  }

  @Override
  public int getNumberOfDataPoints() {
    return intensityValues.length;
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Override
  public double[] getMzValues(@NotNull double[] dst) {
    System.arraycopy(mzValues, 0, dst, 0, mzValues.length);
    return dst;
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    System.arraycopy(intensityValues, 0, dst, 0, intensityValues.length);
    return dst;
  }

  @Override
  public double getMzValue(int index) {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Override
  public double getIntensityValue(int index) {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Nullable
  @Override
  public Double getBasePeakMz() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Nullable
  @Override
  public Double getBasePeakIntensity() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Nullable
  @Override
  public Integer getBasePeakIndex() {
    return basePeakIndex;
  }

  @Nullable
  @Override
  public Range<Double> getDataPointMZRange() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Nullable
  @Override
  public Double getTIC() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Override
  public Stream<DataPoint> stream() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @NotNull
  @Override
  public RawDataFile getDataFile() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Override
  public double getMobility() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Override
  public MobilityType getMobilityType() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Override
  public Frame getFrame() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Override
  public float getRetentionTime() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Override
  public int getMobilityScanNumber() {
    return scanNumber;
  }

  @Nullable
  @Override
  public PasefMsMsInfo getMsMsInfo() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Override
  public void addMassList(@NotNull MassList massList) {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Override
  public MassList getMassList() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @NotNull
  @Override
  public Iterator<DataPoint> iterator() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Override
  public @Nullable Float getInjectionTime() {
    return null;
  }
}
