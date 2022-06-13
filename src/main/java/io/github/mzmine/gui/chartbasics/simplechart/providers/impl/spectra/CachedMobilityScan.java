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

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.util.DataPointUtils;
import java.util.Iterator;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
  private int basePeakIndex;

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

    basePeakIndex = -1;
    double tempTic = 0;
    double max = 0;
    for (int i = 0; i < intensities.length; i++) {
      double intensity = intensities[i];
      tempTic += intensity;
      if (intensity > max) {
        basePeakIndex = i;
        max = intensity;
      }
    }

    tic = tempTic;
  }

  @Override
  public int getNumberOfDataPoints() {
    return mzs.length;
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return originalMobilityScan.getSpectrumType();
  }

  @Override
  public double[] getMzValues(@NotNull double[] dst) {
    System.arraycopy(mzs, 0, dst, 0, mzs.length);
    return dst;
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    System.arraycopy(intensities, 0, dst, 0, intensities.length);
    return dst;
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
    if (basePeakIndex == -1) {
      return 0d;
    }
    return getMzValue(basePeakIndex);
  }

  @Nullable
  @Override
  public Double getBasePeakIntensity() {
    if (basePeakIndex == -1) {
      return 0d;
    }
    return getIntensityValue(basePeakIndex);
  }

  @Nullable
  @Override
  public Integer getBasePeakIndex() {
    return basePeakIndex != -1 ? basePeakIndex : null;
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

  @NotNull
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
  public int getMobilityScanNumber() {
    return originalMobilityScan.getMobilityScanNumber();
  }

  @Nullable
  @Override
  public PasefMsMsInfo getMsMsInfo() {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Override
  public void addMassList(@NotNull MassList massList) {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Override
  public MassList getMassList() {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @NotNull
  @Override
  public Iterator<DataPoint> iterator() {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Override
  public @Nullable Float getInjectionTime() {
    return getFrame().getInjectionTime();
  }
}
