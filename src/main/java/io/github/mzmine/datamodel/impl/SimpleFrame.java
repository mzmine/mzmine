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
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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

  /**
   * key = scan num, value = mobility scan
   */
  private final List<MobilityScan> mobilitySubScans = new ArrayList<>();
  private final MobilityType mobilityType;
  private Set<ImsMsMsInfo> precursorInfos;
  private Range<Double> mobilityRange;

  private DoubleBuffer mobilityBuffer;
  private DoubleBuffer mobilityScanIntensityBuffer;
  private DoubleBuffer mobilityScanMzBuffer;

  public SimpleFrame(@Nonnull RawDataFile dataFile, int scanNumber, int msLevel,
      float retentionTime, double precursorMZ, int precursorCharge, @Nullable double[] mzValues,
      @Nullable double[] intensityValues, MassSpectrumType spectrumType, PolarityType polarity,
      String scanDefinition, @Nonnull Range<Double> scanMZRange, MobilityType mobilityType,
      @Nullable Set<ImsMsMsInfo> precursorInfos) {
    super(dataFile, scanNumber, msLevel, retentionTime, precursorMZ, precursorCharge, /*
         * fragmentScans,
         */
        mzValues, intensityValues, spectrumType, polarity, scanDefinition, scanMZRange);

    this.mobilityType = mobilityType;
    mobilityRange = Range.singleton(0.d);
    this.precursorInfos = Objects.requireNonNullElse(precursorInfos, new HashSet<>());
  }

  public void setDataPoints(double[] newMzValues, double[] newIntensityValues) {
    super.setDataPoints(getDataFile().getMemoryMapStorage(), newMzValues, newIntensityValues);
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
   * @return Collection of mobility sub scans sorted by increasing scan num.
   */
  @Nonnull
  @Override
  public List<MobilityScan> getMobilityScans() {
    return ImmutableList.copyOf(mobilitySubScans);
  }

  /**
   * Not to be used during processing. Can only be called during raw data file reading before
   * finishWriting() was called.
   *
   * @param originalMobilityScans The mobility scans to store.
   */
  public void setMobilityScans(List<BuildingMobilityScan> originalMobilityScans) {
    if (mobilityScanIntensityBuffer != null || mobilityScanMzBuffer != null) {
      throw new IllegalStateException("Mobility scans can only be set to a frame once.");
    }

    // determine offsets for each mobility scan
    final int[] offsets = new int[originalMobilityScans.size()];

    offsets[0] = 0;
    for (int i = 1; i < offsets.length; i++) {
      int oldOffset = offsets[i - 1];
      int numPoints = originalMobilityScans.get(i - 1).getNumberOfDataPoints();
      offsets[i] = oldOffset + numPoints;
    }

    // now create a big array that contains all m/z and intensity values so we can store it in a single buffer
    final int numDatapoints =
        offsets[offsets.length - 1] + originalMobilityScans.get(offsets.length - 1)
            .getNumberOfDataPoints();

    // now store all the data in a single array
    double[] data = new double[numDatapoints];
    int dpCounter = 0;
    for (int i = 0; i < originalMobilityScans.size(); i++) {
      BuildingMobilityScan currentScan = originalMobilityScans.get(i);
      double[] currentIntensities = currentScan.getIntensityValues();
      for (int j = 0; j < currentIntensities.length; j++) {
        data[dpCounter] = currentIntensities[j];
        dpCounter++;
      }
    }

    mobilityScanIntensityBuffer = StorageUtils.storeValuesToDoubleBuffer(getDataFile().getMemoryMapStorage(), data);
    if(getDataFile().getMemoryMapStorage() == null) {
      data = new double[numDatapoints]; // cannot reuse the same array then
    }
    // same for mzs
    dpCounter = 0;
    for (int i = 0; i < originalMobilityScans.size(); i++) {
      BuildingMobilityScan currentScan = originalMobilityScans.get(i);
      double[] currentMzs = currentScan.getMzValues();
      for (int j = 0; j < currentMzs.length; j++) {
        data[dpCounter] = currentMzs[j];
        dpCounter++;
      }
    }

    mobilityScanMzBuffer =
        StorageUtils.storeValuesToDoubleBuffer(getDataFile().getMemoryMapStorage(), data);

    // now create the scans
    for (int i = 0; i < originalMobilityScans.size(); i++) {
      MobilityScan scan = originalMobilityScans.get(i);
      mobilitySubScans.add(new SimpleMobilityScan(scan.getMobilityScanNumber(), this, offsets[i],
          scan.getNumberOfDataPoints(), scan.getBasePeakIndex()));
    }
  }

  @Override
  public double getMobilityForMobilityScanNumber(int mobilityScanIndex) {
    return mobilityBuffer.get(mobilityScanIndex);
  }

  @Override
  public double getMobilityForMobilityScan(MobilityScan scan) {
    // correct the index with an offset in case there is one.
    int index = mobilitySubScans.indexOf(scan) - mobilitySubScans.get(0).getMobilityScanNumber();
    if (index >= 0) {
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
    return precursorInfos;
  }

  @Nullable
  @Override
  public ImsMsMsInfo getImsMsMsInfoForMobilityScan(int mobilityScanNumber) {
    if (precursorInfos == null) {
      return null;
    }
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
    mobilityBuffer = StorageUtils.storeValuesToDoubleBuffer(getDataFile().getMemoryMapStorage(),
        mobilities);
    mobilityRange = Range.singleton(mobilities[0]);
    mobilityRange = mobilityRange.span(Range.singleton(mobilities[mobilities.length - 1]));
    return mobilityBuffer;
  }

  public void setPrecursorInfos(@Nullable Set<ImsMsMsInfo> precursorInfos) {
    this.precursorInfos = precursorInfos;
  }

  void getMobilityScanMzValues(SimpleMobilityScan scan, double[] dst) {
    assert scan.getNumberOfDataPoints() <= dst.length;
    mobilityScanMzBuffer.get(scan.getStorageOffset(), dst, 0, scan.getNumberOfDataPoints());
  }

  void getMobilityScanIntensityValues(SimpleMobilityScan scan, double[] dst) {
    assert scan.getNumberOfDataPoints() <= dst.length;
    mobilityScanIntensityBuffer.get(scan.getStorageOffset(), dst, 0, scan.getNumberOfDataPoints());
  }

  double getMobilityScanMzValue(SimpleMobilityScan scan, int index) {
    assert index < scan.getNumberOfDataPoints();
    return mobilityScanMzBuffer.get(scan.getStorageOffset() + index);
  }

  double getMobilityScanIntensityValue(SimpleMobilityScan scan, int index) {
    assert index < scan.getNumberOfDataPoints();
    return mobilityScanIntensityBuffer.get(scan.getStorageOffset() + index);
  }

}
