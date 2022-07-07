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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import it.unimi.dsi.fastutil.doubles.DoubleImmutableList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author https://github.com/SteffenHeu
 * @see Frame
 */
public class SimpleFrame extends SimpleScan implements Frame {

  private final MobilityType mobilityType;

  @NotNull
  private Set<PasefMsMsInfo> precursorInfos;
  private Range<Double> mobilityRange;

  private int mobilitySegment = -1;
  private MobilityScanStorage mobilityScanStorage;

  public SimpleFrame(@NotNull RawDataFile dataFile, int scanNumber, int msLevel,
      float retentionTime, @Nullable double[] mzValues, @Nullable double[] intensityValues,
      MassSpectrumType spectrumType, PolarityType polarity, String scanDefinition,
      @NotNull Range<Double> scanMZRange, MobilityType mobilityType,
      @Nullable Set<PasefMsMsInfo> precursorInfos, Float accumulationTime) {
    super(dataFile, scanNumber, msLevel, retentionTime, null, /*
         * fragmentScans,
         */
        mzValues, intensityValues, spectrumType, polarity, scanDefinition, scanMZRange, accumulationTime);

    this.mobilityType = mobilityType;
    mobilityRange = Range.singleton(0.d);
    this.precursorInfos = Objects.requireNonNullElse(precursorInfos, new HashSet<>(0));
  }

  public void setDataPoints(double[] newMzValues, double[] newIntensityValues) {
    super.setDataPoints(getDataFile().getMemoryMapStorage(), newMzValues, newIntensityValues);
    // update afterwards, an assertion might be triggered.
    ((IMSRawDataFileImpl) getDataFile()).updateMaxRawDataPoints(newIntensityValues.length);
  }

  /**
   * @return The number of mobility resolved sub scans.
   */
  @Override
  public int getNumberOfMobilityScans() {
    return mobilityScanStorage.getNumberOfMobilityScans();
  }

  @Override
  @NotNull
  public MobilityType getMobilityType() {
    return mobilityType;
  }

  @Override
  @NotNull
  public Range<Double> getMobilityRange() {
    if (mobilityRange != null) {
      return mobilityRange;
    }
    return Range.singleton(0.0);
  }

  public MobilityScanStorage getMobilityScanStorage() {
    if (mobilityScanStorage == null) {
      throw new IllegalStateException("Mobility scans not loaded during file import.");
    }
    return mobilityScanStorage;
  }

  @NotNull
  @Override
  public MobilityScan getMobilityScan(int num) {
    return getMobilityScanStorage().getMobilityScan(num);
  }

  /**
   * @return Collection of mobility sub scans sorted by increasing scan num.
   */
  @NotNull
  @Override
  public List<MobilityScan> getMobilityScans() {
    return getMobilityScanStorage().getMobilityScans();
  }

  /**
   * Not to be used during processing. Can only be called during raw data file reading before
   * finishWriting() was called.
   *
   * @param originalMobilityScans The mobility scans to store.
   */
  public void setMobilityScans(List<BuildingMobilityScan> originalMobilityScans,
      boolean useAsMassList) {
    if (getMobilities() != null && (originalMobilityScans.size() != getMobilities().size())) {
      throw new IllegalArgumentException(String.format(
          "Number of mobility values (%d) does not match number of mobility scans (%d).",
          getMobilities().size(), originalMobilityScans.size()));
    }
    mobilityScanStorage = new MobilityScanStorage(getDataFile().getMemoryMapStorage(), this,
        originalMobilityScans, useAsMassList);
  }

  @Override
  public double getMobilityForMobilityScanNumber(int mobilityScanIndex) {
    return ((IMSRawDataFile) (getDataFile())).getSegmentMobilities(
        mobilitySegment).getDouble(mobilityScanIndex);
  }

  @Override
  public @Nullable DoubleImmutableList getMobilities() {
    if (mobilitySegment == -1) {
      return null;
    }
    return ((IMSRawDataFile) (getDataFile())).getSegmentMobilities(mobilitySegment);
  }

  @NotNull
  @Override
  public Set<PasefMsMsInfo> getImsMsMsInfos() {
    return precursorInfos;
  }

  @Nullable
  @Override
  public PasefMsMsInfo getImsMsMsInfoForMobilityScan(int mobilityScanNumber) {
    if (precursorInfos == null) {
      return null;
    }
    Optional<PasefMsMsInfo> pcInfo = precursorInfos.stream()
        .filter(info -> info.getSpectrumNumberRange().contains(mobilityScanNumber)).findFirst();
    return pcInfo.orElse(null);
  }

  @Override
  public List<MobilityScan> getSortedMobilityScans() {
    List<MobilityScan> result = new ArrayList<>(getMobilityScans());
    result.sort(Comparator.comparingDouble(MobilityScan::getMobility));
    return ImmutableList.copyOf(result);
  }

  public int setMobilities(double[] mobilities) {
    mobilitySegment = ((IMSRawDataFile) getDataFile()).addMobilityValues(mobilities);
    mobilityRange = Range.singleton(mobilities[0]);
    mobilityRange = mobilityRange.span(Range.singleton(mobilities[mobilities.length - 1]));
    return mobilitySegment;
  }

  public void setPrecursorInfos(@Nullable Set<PasefMsMsInfo> precursorInfos) {
    this.precursorInfos = precursorInfos != null ? precursorInfos : new HashSet<>();
  }

  /**
   * @return The maximum number of data points in a mobility scan in this frame. -1 If no mobility
   * scans have been added.
   */
  @Override
  public int getMaxMobilityScanRawDataPoints() {
    if (mobilityScanStorage == null) {
      throw new IllegalStateException("Mobility scans not set");
    }
    return mobilityScanStorage.getRawMaxNumPoints();
  }

  @Override
  public int getTotalMobilityScanRawDataPoints() {
    if (mobilityScanStorage == null) {
      throw new IllegalStateException("Mobility scans not set");
    }
    return mobilityScanStorage.getRawTotalNumPoints();
  }

  /**
   * @return The maximum number of data points in a mobility scan in this frame. -1 If no mobility
   * scans have been added.
   */
  @Override
  public int getMaxMobilityScanMassListDataPoints() {
    if (mobilityScanStorage == null) {
      throw new IllegalStateException("Mobility scans not set");
    }
    return mobilityScanStorage.getMassListMaxNumPoints();
  }

  @Override
  public int getTotalMobilityScanMassListDataPoints() {
    if (mobilityScanStorage == null) {
      throw new IllegalStateException("Mobility scans not set");
    }
    return mobilityScanStorage.getMassListTotalNumPoints();
  }
}
