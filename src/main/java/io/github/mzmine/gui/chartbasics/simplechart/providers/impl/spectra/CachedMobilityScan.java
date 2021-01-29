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

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.util.DataPointUtils;
import java.nio.DoubleBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Loads a frame and it's subscans into ram.
 *
 * @author https://github.com/SteffenHeu
 */
public class CachedMobilityScan implements MobilityScan {

  private final MobilityScan originalMobilityScan;
  private final double[] mzs;
  private final double[] intensities;
  private final double tic;

  public CachedMobilityScan(MobilityScan scan, double noiseLevel) {
    this.originalMobilityScan = scan;

    double[] allmz = new double[scan.getNumberOfDataPoints()];
    double[] allintensities = new double[scan.getNumberOfDataPoints()];
    scan.getMzValues(allmz);
    scan.getIntensityValues(allintensities);

    double[][] data = DataPointUtils
        .getDatapointsAboveNoiseLevel(allmz, allintensities, noiseLevel);

    mzs = data[0];
    intensities = data[1];
    tic = Arrays.stream(intensities).sum();
  }

  @Override
  public int getNumberOfDataPoints() {
    return mzs.length;
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return originalMobilityScan.getSpectrumType();
  }

  @Nonnull
  @Override
  public DoubleBuffer getMzValues() {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Nonnull
  @Override
  public DoubleBuffer getIntensityValues() {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Override
  public double[] getMzValues(@Nonnull double[] dst) {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Override
  public double[] getIntensityValues(@Nonnull double[] dst) {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Override
  public double getMzValue(int index) {
    return mzs[index];
  }

  @Override
  public double getIntensityValue(int index) {
    return intensities[index];
  }

  @Nullable
  @Override
  public Double getBasePeakMz() {
    return originalMobilityScan.getBasePeakMz();
  }

  @Nullable
  @Override
  public Double getBasePeakIntensity() {
    return originalMobilityScan.getBasePeakIntensity();
  }

  @Nullable
  @Override
  public Integer getBasePeakIndex() {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Nullable
  @Override
  public Range<Double> getDataPointMZRange() {
    return originalMobilityScan.getDataPointMZRange();
  }

  @Nullable
  @Override
  public Double getTIC() {
    return tic;
  }

  @Override
  public Stream<DataPoint> stream() {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Nonnull
  @Override
  public RawDataFile getDataFile() {
    return originalMobilityScan.getDataFile();
  }

  @Override
  public double getMobility() {
    return originalMobilityScan.getMobility();
  }

  @Override
  public MobilityType getMobilityType() {
    return originalMobilityScan.getMobilityType();
  }

  @Override
  public Frame getFrame() {
    return originalMobilityScan.getFrame();
  }

  @Override
  public float getRetentionTime() {
    return originalMobilityScan.getRetentionTime();
  }

  @Override
  public int getMobilityScamNumber() {
    return originalMobilityScan.getMobilityScamNumber();
  }

  @Nullable
  @Override
  public ImsMsMsInfo getMsMsInfo() {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Override
  public void addMassList(@Nonnull MassList massList) {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Override
  public void removeMassList(@Nonnull MassList massList) {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Nonnull
  @Override
  public Set<MassList> getMassLists() {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Override
  public MassList getMassList(@Nonnull String name) {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Nonnull
  @Override
  public Iterator<DataPoint> iterator() {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }
}
