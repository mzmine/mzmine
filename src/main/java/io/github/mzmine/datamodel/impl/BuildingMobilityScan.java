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

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import java.nio.DoubleBuffer;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * During raw data import, we need to cache the m/z and intensity values of mobility scans, so we
 * can store all m/zs and intensities of a frame in a single double buffer to save memory.
 * Therefore, we only implement the basic functionality here to limit the usage of this class to the
 * loading process.
 */
public class BuildingMobilityScan implements MobilityScan {

  final int scanNumber;
  final double[] intensities;
  final double[] mzs;

  public BuildingMobilityScan(int scanNumber, double[] intensities, double[] mzs) {
    this.scanNumber = scanNumber;
    this.intensities = intensities;
    this.mzs = mzs;
  }

  public double[] getMzs() {
    return mzs;
  }

  public double[] getIntensities() {
    return intensities;
  }

  @Override
  public int getNumberOfDataPoints() {
    return intensities.length;
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Nonnull
  @Override
  public DoubleBuffer getMzValues() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Nonnull
  @Override
  public DoubleBuffer getIntensityValues() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Override
  public double[] getMzValues(@Nonnull double[] dst) {
    // we store arrays anyway, so no point in making the user allocate a new one
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Override
  public double[] getIntensityValues(@Nonnull double[] dst) {
    // we store arrays anyway, so no point in making the user allocate a new one
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
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
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
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

  @Nonnull
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
  public int getMobilityScamNumber() {
    return scanNumber;
  }

  @Nullable
  @Override
  public ImsMsMsInfo getMsMsInfo() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Override
  public void addMassList(@Nonnull MassList massList) {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Override
  public void removeMassList(@Nonnull MassList massList) {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Nonnull
  @Override
  public Set<MassList> getMassLists() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Override
  public MassList getMassList(@Nonnull String name) {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }

  @Nonnull
  @Override
  public Iterator<DataPoint> iterator() {
    throw new UnsupportedOperationException("Not supported by " + this.getClass().getName());
  }
}
