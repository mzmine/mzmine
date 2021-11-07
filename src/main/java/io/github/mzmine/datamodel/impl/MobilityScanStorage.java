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

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.datamodel.impl.masslist.StoredMobilityScanMassList;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MobilityScanStorage {

  // raw data
  private final Frame frame;
  private final DoubleBuffer rawMzValues;
  private final DoubleBuffer rawIntensityValues;
  private final int[] rawStorageOffsets;
  private final int[] rawBasePeakIndices;
  private final int rawMaxNumPoints;

  // mass list
  private DoubleBuffer massListMzValues = null;
  private DoubleBuffer massListIntensityValues = null;
  private int[] massListStorageOffsets = null;
  private int[] massListBasePeakIndices = null;
  private int massListMaxNumPoints = -1;

  public MobilityScanStorage(@Nullable MemoryMapStorage storage, @NotNull final Frame frame,
      @NotNull final List<BuildingMobilityScan> mobilityScans) {
    for (int i = 0; i < mobilityScans.size(); i++) {
      if (mobilityScans.get(i).getMobilityScanNumber() != i) {
        throw new IllegalArgumentException(
            "Mobility scan numbers for a frame must start with zero and be consecutive.");
      }
    }

    this.frame = frame;

    final List<double[][]> data = StorageUtils.mapTo2dDoubleArrayList(mobilityScans,
        BuildingMobilityScan::getMzValues, BuildingMobilityScan::getIntensityValues);

    final AtomicInteger biggestOffset = new AtomicInteger(0);
    rawStorageOffsets = StorageUtils.generateOffsets(data, biggestOffset);
    rawMaxNumPoints = biggestOffset.get();

    final int numDp =
        rawStorageOffsets[rawStorageOffsets.length - 1] + data.get(data.size() - 1)[0].length;
    final double[] mzs = new double[numDp];
    final double[] intensities = new double[numDp];

    StorageUtils.putAllValuesIntoOneArray(data, 0, mzs);
    rawBasePeakIndices = StorageUtils.putAllValuesIntoOneArray(data, 1, intensities);
    rawMzValues = StorageUtils.storeValuesToDoubleBuffer(storage, mzs);
    rawIntensityValues = StorageUtils.storeValuesToDoubleBuffer(storage, intensities);
  }

  public MobilityScanStorage(@Nullable MemoryMapStorage storage, @NotNull final Frame frame,
      @NotNull final List<BuildingMobilityScan> mobilityScans, boolean useAsMassList) {
    this(storage, frame, mobilityScans);

    if (useAsMassList) {
      massListBasePeakIndices = rawBasePeakIndices;
      massListMaxNumPoints = rawMaxNumPoints;
      massListMzValues = rawMzValues;
      massListIntensityValues = rawIntensityValues;
      massListStorageOffsets = rawStorageOffsets;
    }
  }

  public void generateAndAddMobilityScanMassLists(@Nullable MemoryMapStorage storage,
      @NotNull MassDetector massDetector, @NotNull ParameterSet massDetectorParameters) {

    // mobility scan -> [0][] = mzs, [1][] = intensities
    final List<double[][]> data = new ArrayList<>();

    for (MobilityScan mobilityScan : getMobilityScans()) {
      double[][] mzIntensity = massDetector.getMassValues(mobilityScan, massDetectorParameters);
      data.add(mzIntensity);
    }

    AtomicInteger biggestOffset = new AtomicInteger(0);
    massListStorageOffsets = StorageUtils.generateOffsets(data, biggestOffset);
    massListMaxNumPoints = biggestOffset.get();

    final int numDp = massListStorageOffsets[massListStorageOffsets.length - 1] + data.get(
        data.size() - 1)[0].length;
    final double[] mzs = new double[numDp];
    final double[] intensities = new double[numDp];

    StorageUtils.putAllValuesIntoOneArray(data, 0, mzs);
    massListBasePeakIndices = StorageUtils.putAllValuesIntoOneArray(data, 1, intensities);
    massListMzValues = StorageUtils.storeValuesToDoubleBuffer(storage, mzs);
    massListIntensityValues = StorageUtils.storeValuesToDoubleBuffer(storage, intensities);
  }

  public MassList getMassList(int mobilityScanIndex) {
    if (massListIntensityValues == null) {
      return null;
    }
    return new StoredMobilityScanMassList(mobilityScanIndex, this);
  }

  public MobilityScan getMobilityScan(int index) {
    assert index <= getNumberOfMobilityScans();
    return new StoredMobilityScan(index, this);
  }

  public List<MobilityScan> getMobilityScans() {
    List<MobilityScan> scans = new ArrayList<>(getNumberOfMobilityScans());
    for (int i = 0; i < getNumberOfMobilityScans(); i++) {
      scans.add(new StoredMobilityScan(i, this));
    }
    return scans;
  }

  public int getNumberOfMobilityScans() {
    return rawStorageOffsets.length;
  }

  /**
   * @param index The mobility scan index.
   * @return The number of points in the mobility scan.
   */
  public int getNumberOfRawDatapoints(int index) {
    assert index < getNumberOfMobilityScans();
    if (index < rawStorageOffsets.length - 1) {
      return rawStorageOffsets[index + 1] - rawStorageOffsets[index];
    } else {
      return rawMzValues.capacity() - rawStorageOffsets[index];
    }
  }

  /**
   * @param index The mobility scan index.
   * @return The base peak index or null.
   */
  @Nullable
  public Integer getRawBasePeakIndex(int index) {
    assert index < getNumberOfMobilityScans();
    return rawBasePeakIndices[index] != -1 ? rawBasePeakIndices[index] : null;
  }

  /**
   * @return The maximum number of data points in a single mobility scan.
   */
  public int getRawMaxNumPoints() {
    return rawMaxNumPoints;
  }

  /**
   * @return The total number of points in this {@link  MobilityScanStorage}.
   */
  public int getRawTotalNumPoints() {
    return rawMzValues.capacity();
  }

  public Frame getFrame() {
    return frame;
  }

  public int getRawStorageOffset(int mobilityScanIndex) {
    assert mobilityScanIndex < getNumberOfMobilityScans();
    return rawStorageOffsets[mobilityScanIndex];
  }

  public void getRawMobilityScanMzValues(int mobilityScanIndex, double[] dst, int dstPos) {
    assert getNumberOfRawDatapoints(mobilityScanIndex) + dstPos <= dst.length;
    rawMzValues.get(getRawStorageOffset(mobilityScanIndex), dst, dstPos,
        getNumberOfRawDatapoints(mobilityScanIndex));
  }

  public void getRawMobilityScanIntensityValues(int mobilityScanIndex, double[] dst, int dstPos) {
    assert getNumberOfRawDatapoints(mobilityScanIndex) + dstPos <= dst.length;
    rawIntensityValues.get(getRawStorageOffset(mobilityScanIndex), dst, dstPos,
        getNumberOfRawDatapoints(mobilityScanIndex));
  }

  public double getRawMobilityScanMzValue(int mobilityScanIndex, int index) {
    return rawMzValues.get(getRawStorageOffset(mobilityScanIndex) + index);
  }

  public double getRawMobilityScanIntensityValue(int mobilityScanIndex, int index) {
    return rawIntensityValues.get(getRawStorageOffset(mobilityScanIndex) + index);
  }

  // mass list

  public int getNumberOfMassListDatapoints(int index) {
    assert index < getNumberOfMobilityScans();
    if (index < massListStorageOffsets.length - 1) {
      return massListStorageOffsets[index + 1] - massListStorageOffsets[index];
    } else {
      return massListMzValues.capacity() - massListStorageOffsets[index];
    }
  }

  public int getMassListStorageOffset(int index) {
    return massListStorageOffsets[index];
  }

  public int getMassListBasePeakIndex(int index) {
    return massListBasePeakIndices[index];
  }

  public int getMassListMaxNumPoints() {
    return massListMaxNumPoints;
  }

  public int getMassListTotalNumPoints() {
    return massListIntensityValues.capacity();
  }

  public void getMassListMzValues(int mobilityScanIndex, double[] dst, int dstPos) {
    assert getNumberOfMassListDatapoints(mobilityScanIndex) + dstPos <= dst.length;
    massListMzValues.get(getMassListStorageOffset(mobilityScanIndex), dst, dstPos,
        getNumberOfMassListDatapoints(mobilityScanIndex));
  }

  public void getMassListIntensityValues(int mobilityScanIndex, double[] dst, int dstPos) {
    assert getNumberOfMassListDatapoints(mobilityScanIndex) + dstPos <= dst.length;
    massListIntensityValues.get(getMassListStorageOffset(mobilityScanIndex), dst, dstPos,
        getNumberOfMassListDatapoints(mobilityScanIndex));
  }

  public double getMassListMzValue(int mobilityScanIndex, int index) {
    return massListMzValues.get(getMassListStorageOffset(mobilityScanIndex) + index);
  }

  public double getMassListIntensityValue(int mobilityScanIndex, int index) {
    return massListIntensityValues.get(getMassListStorageOffset(mobilityScanIndex) + index);
  }
}
