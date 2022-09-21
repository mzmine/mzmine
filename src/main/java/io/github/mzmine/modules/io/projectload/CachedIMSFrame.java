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

package io.github.mzmine.modules.io.projectload;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.MobilityScanStorage;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import it.unimi.dsi.fastutil.doubles.DoubleImmutableList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class is used during project import to cache {@link io.github.mzmine.datamodel.impl.StoredMobilityScan}s.
 * Since mobility scans are created on demand of every call to {@link Frame#getMobilityScans()},
 * multiple duplicate instances would be created during project import, for every loaded {@link
 * io.github.mzmine.datamodel.featuredata.IonSpectrumSeries}, every {@link
 * io.github.mzmine.datamodel.features.types.numbers.BestScanNumberType}, and so on. This class
 * contains {@link CachedIMSFrame}s over regular frames. These cached frames will once create and
 * retain instances of {@link io.github.mzmine.datamodel.impl.StoredMobilityScan}s so the same
 * instance can be used throughout the loading process.
 *
 * @author SteffenHeu https://github.com/SteffenHeu
 */
public class CachedIMSFrame implements Frame {

  private final Frame originalFrame;
  private List<MobilityScan> cachedScans = null;

  public CachedIMSFrame(Frame frame) {
    originalFrame = frame;
  }

  @NotNull
  public Frame getOriginalFrame() {
    return originalFrame;
  }

  @Override
  public int getNumberOfMobilityScans() {
    return originalFrame.getNumberOfMobilityScans();
  }

  @Override
  public @NotNull MobilityType getMobilityType() {
    return originalFrame.getMobilityType();
  }

  @Override
  public @NotNull Range<Double> getMobilityRange() {
    return originalFrame.getMobilityRange();
  }

  @Override
  public @Nullable MobilityScan getMobilityScan(int num) {
    if(cachedScans == null) {
      cachedScans = originalFrame.getMobilityScans();
    }
    return cachedScans.get(num);
  }

  @Override
  public @NotNull List<MobilityScan> getMobilityScans() {
    if(cachedScans == null) {
      cachedScans = originalFrame.getMobilityScans();
    }
    return cachedScans;
  }

  @Override
  public @NotNull List<MobilityScan> getSortedMobilityScans() {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public MobilityScanStorage getMobilityScanStorage() {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public double getMobilityForMobilityScanNumber(int mobilityScanIndex) {
    return originalFrame.getMobilityForMobilityScanNumber(mobilityScanIndex);
  }

  @Override
  public @Nullable DoubleImmutableList getMobilities() {
    return originalFrame.getMobilities();
  }

  @Override
  public @NotNull Set<PasefMsMsInfo> getImsMsMsInfos() {
    return originalFrame.getImsMsMsInfos();
  }

  @Override
  public @Nullable PasefMsMsInfo getImsMsMsInfoForMobilityScan(int mobilityScanNumber) {
    return originalFrame.getImsMsMsInfoForMobilityScan(mobilityScanNumber);
  }

  @Override
  public int getMaxMobilityScanRawDataPoints() {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public int getTotalMobilityScanRawDataPoints() {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public int getMaxMobilityScanMassListDataPoints() {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public int getTotalMobilityScanMassListDataPoints() {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public int getNumberOfDataPoints() {
    return originalFrame.getNumberOfDataPoints();
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return originalFrame.getSpectrumType();
  }

  @Override
  public double[] getMzValues(@NotNull double[] dst) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public double getMzValue(int index) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public double getIntensityValue(int index) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @Nullable Double getBasePeakMz() {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @Nullable Double getBasePeakIntensity() {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @Nullable Integer getBasePeakIndex() {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @Nullable Range<Double> getDataPointMZRange() {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @Nullable Double getTIC() {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public Stream<DataPoint> stream() {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @NotNull RawDataFile getDataFile() {
    return originalFrame.getDataFile();
  }

  @Override
  public int getScanNumber() {
    return originalFrame.getScanNumber();
  }

  @Override
  public @NotNull String getScanDefinition() {
    return originalFrame.getScanDefinition();
  }

  @Override
  public int getMSLevel() {
    return originalFrame.getMSLevel();
  }

  @Override
  public float getRetentionTime() {
    return originalFrame.getRetentionTime();
  }

  @Override
  public @NotNull Range<Double> getScanningMZRange() {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @Nullable MsMsInfo getMsMsInfo() {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @NotNull PolarityType getPolarity() {
    return originalFrame.getPolarity();
  }

  @Override
  public @Nullable MassList getMassList() {
    return originalFrame.getMassList();
  }

  @Override
  public void addMassList(@NotNull MassList massList) {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @NotNull
  @Override
  public Iterator<DataPoint> iterator() {
    throw new UnsupportedOperationException("Unsupported during project load.");
  }

  @Override
  public @Nullable Float getInjectionTime() {
    return originalFrame.getInjectionTime();
  }
}
