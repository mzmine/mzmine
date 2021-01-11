/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author https://github.com/SteffenHeu
 * @see Frame
 */
public class SimpleFrame extends SimpleScan implements Frame {

//  private final int numMobilitySpectra;
  /**
   * key = scan num, value = mobility scan
   */
  private final List<MobilityScan> mobilitySubScans = new ArrayList<>();
  private final MobilityType mobilityType;
//  private final Map<Integer, Double> mobilities;
  private final Set<ImsMsMsInfo> precursorInfos;
  private Range<Double> mobilityRange;

  private DoubleBuffer mobilityBuffer;

  public SimpleFrame(@Nonnull RawDataFile dataFile, int scanNumber, int msLevel,
      float retentionTime, double precursorMZ, int precursorCharge, DataPoint dps[],
      MassSpectrumType spectrumType, PolarityType polarity, String scanDefinition,
      @Nonnull Range<Double> scanMZRange, MobilityType mobilityType, final int numMobilitySpectra,
      @Nonnull Map<Integer, Double> mobilities, @Nullable Set<ImsMsMsInfo> precursorInfos) {
    super(dataFile, scanNumber, msLevel, retentionTime, precursorMZ, precursorCharge, /*
         * fragmentScans,
         */
        null, null, spectrumType, polarity, scanDefinition, scanMZRange);

    setDataPoints(dps);
    this.mobilityType = mobilityType;
    mobilityRange = Range.singleton(0.d);
//    this.numMobilitySpectra = numMobilitySpectra;
//    this.mobilities = mobilities;
    this.precursorInfos = precursorInfos;
  }

  public SimpleFrame(@Nonnull RawDataFile dataFile, int scanNumber, int msLevel,
      float retentionTime, double precursorMZ, int precursorCharge, double[] mzValues,
      double[] intensityValues, MassSpectrumType spectrumType, PolarityType polarity,
      String scanDefinition, @Nonnull Range<Double> scanMZRange, MobilityType mobilityType,
      final int numMobilitySpectra, @Nonnull Map<Integer, Double> mobilities,
      @Nullable Set<ImsMsMsInfo> precursorInfos) {
    super(dataFile, scanNumber, msLevel, retentionTime, precursorMZ, precursorCharge, /*
         * fragmentScans,
         */
        null, null, spectrumType, polarity, scanDefinition, scanMZRange);

    setDataPoints(mzValues, intensityValues);
    this.mobilityType = mobilityType;
    mobilityRange = Range.singleton(0.d);
//    this.numMobilitySpectra = numMobilitySpectra;
//    this.mobilities = mobilities;
    this.precursorInfos = precursorInfos;
  }

  /**
   * @return The number of mobility resolved sub scans.
   */
  @Override
  public int getNumberOfMobilityScans() {
    return mobilitySubScans.size();
  }

  @Override
  @Nonnull
  public MobilityType getMobilityType() {
    return mobilityType;
  }

  /**
   * @return Scan numbers of sub scans.
   */
//  @Override
//  public Set<Integer> getMobilityScanNumbers() {
//    return mobilities.keySet();
//  }

  @Override
  @Nonnull
  public Range<Double> getMobilityRange() {
    if (mobilityRange != null) {
      return mobilityRange;
    }
    return Range.singleton(0.0);
  }

  @Nonnull
  @Override
  public MobilityScan getMobilityScan(int num) {
    return Objects.requireNonNull(mobilitySubScans.get(num));
  }

  /**
   * Not to be used during processing. Can only be called during raw data file reading before
   * finishWriting() was called.
   *
   * @param originalMobilityScan The mobility scan to store.
   */
  @Override
  public void addMobilityScan(MobilityScan originalMobilityScan) {

//    if (mobilityRange == null) {
//      mobilityRange = Range.singleton(originalMobilityScan.getMobility());
//    } else if (!mobilityRange.contains(originalMobilityScan.getMobility())) {
//      mobilityRange = mobilityRange.span(Range.singleton(originalMobilityScan.getMobility()));
//    }

    mobilitySubScans.add(originalMobilityScan);
  }

  /**
   * @return Collection of mobility sub scans sorted by increasing scan num.
   */
  @Nonnull
  @Override
  public List<MobilityScan> getMobilityScans() {
    return ImmutableList.copyOf(mobilitySubScans);
  }

  @Override
  public double getMobilityForMobilityScanNumber(int mobilityScanIndex) {
//    return mobilities.getOrDefault(mobilityScanIndex, MobilityScan.DEFAULT_MOBILITY);
    return mobilityBuffer.get(mobilityScanIndex);
  }

  @Override
  public double getMobilityForMobilityScan(MobilityScan scan) {
    int index = mobilitySubScans.indexOf(scan);
    if(index != -1) {
      return mobilityBuffer.get(index);
    }
    throw new IllegalArgumentException("Mobility scan does not belong to this frame.");
  }

  @Override
  public DoubleBuffer getMobilities() {
    return mobilityBuffer;
  }

  @Nonnull
  @Override
  public Set<ImsMsMsInfo> getImsMsMsInfos() {
    return Objects.requireNonNullElse(precursorInfos, Collections.emptySet());
  }

  @Nullable
  @Override
  public ImsMsMsInfo getImsMsMsInfoForMobilityScan(int mobilityScanNumber) {
    Optional<ImsMsMsInfo> pcInfo = precursorInfos.stream()
        .filter(info -> info.getSpectrumNumberRange().contains(mobilityScanNumber)).findFirst();
    return pcInfo.orElse(null);
  }

  @Override
  public List<MobilityScan> getSortedMobilityScans() {
    List<MobilityScan> result = new ArrayList<>(mobilitySubScans);
    result.sort(Comparator.comparingDouble(MobilityScan::getMobility));
    return ImmutableList.copyOf(result);
  }

  public DoubleBuffer setMobilities(double[] mobilities) {
    try {
      mobilityBuffer = getDataFile().getMemoryMapStorage().storeData(mobilities);
    } catch (IOException e) {
      e.printStackTrace();
      mobilityBuffer = DoubleBuffer.wrap(mobilities);
    }
    if(mobilities.length != mobilitySubScans.size()) {
      System.out.println("Mobility length does not match number of mobility scans.");
    }

    mobilityRange = Range.singleton(mobilities[0]);
    mobilityRange = mobilityRange.span(Range.singleton(mobilities[mobilities.length - 1]));
    return mobilityBuffer;
  }

  /*
   * @Override public boolean equals(Object o) { if (this == o) { return true; } if (!(o instanceof
   * SimpleFrame)) { return false; } SimpleFrame that = (SimpleFrame) o; return getScanNumber() ==
   * that.getScanNumber() && getMSLevel() == that.getMSLevel() &&
   * Double.compare(that.getPrecursorMZ(), getPrecursorMZ()) == 0 && getPrecursorCharge() ==
   * that.getPrecursorCharge() && Float.compare(that.getRetentionTime(), getRetentionTime()) == 0 &&
   * getNumberOfDataPoints() == that.getNumberOfDataPoints() &&
   * Objects.equals(getDataPointMZRange(), that.getDataPointMZRange()) &&
   * Objects.equals(getHighestDataPoint(), that.getHighestDataPoint()) && Double.compare(getTIC(),
   * that.getTIC()) == 0 && getSpectrumType() == that.getSpectrumType() &&
   * getDataFile().equals(that.getDataFile()) && Objects.equals(getMassLists(), that.getMassLists())
   * && getPolarity() == that.getPolarity() && Objects.equals(getScanDefinition(),
   * that.getScanDefinition()) && getScanningMZRange().equals(that.getScanningMZRange()) &&
   * getMobilityType() == that.getMobilityType() && getFrameId() == that.getFrameId(); }
   *
   * @Override public int hashCode() { return Objects.hash(getScanNumber(), getMSLevel(),
   * getPrecursorMZ(), getPrecursorCharge(), getRetentionTime(), getDataPointMZRange(),
   * getHighestDataPoint(), getTIC(), getSpectrumType(), getNumberOfDataPoints(), getDataFile(),
   * getMassLists(), getPolarity(), getScanDefinition(), getScanningMZRange(), getMobilityType(),
   * getFrameId()); }
   */
}
