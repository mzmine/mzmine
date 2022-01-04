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
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.datamodel.impl.masslist.StoredMobilityScanMassList;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.MemoryMapStorage;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
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
  private final IntBuffer rawStorageOffsets;
  private final IntBuffer rawBasePeakIndices;
  private final int rawMaxNumPoints;

  // mass list
  private DoubleBuffer massListMzValues = null;
  private DoubleBuffer massListIntensityValues = null;
  private IntBuffer massListStorageOffsets = null;
  private IntBuffer massListBasePeakIndices = null;
  private int massListMaxNumPoints = -1;

  public MobilityScanStorage(@Nullable MemoryMapStorage storage, @NotNull final Frame frame,
      @NotNull final List<BuildingMobilityScan> mobilityScans) {
    for (int i = 0; i < mobilityScans.size(); i++) {
      if (mobilityScans.get(i).getMobilityScanNumber() != i) {
        throw new IllegalArgumentException(String.format(
            "Mobility scan numbers for a frame must start with zero and be consecutive. Expected scan number %d, recieved %d",
            i, mobilityScans.get(i).getMobilityScanNumber()));
      }
    }

    this.frame = frame;

    final List<double[][]> data = StorageUtils.mapTo2dDoubleArrayList(mobilityScans,
        BuildingMobilityScan::getMzValues, BuildingMobilityScan::getIntensityValues);

    final AtomicInteger biggestOffset = new AtomicInteger(0);
    final int[] rawStorageOffsets = StorageUtils.generateOffsets(data, biggestOffset);
    this.rawStorageOffsets = StorageUtils.storeValuesToIntBuffer(storage, rawStorageOffsets);

    rawMaxNumPoints = biggestOffset.get();

    final int numDp =
        rawStorageOffsets[rawStorageOffsets.length - 1] + data.get(data.size() - 1)[0].length;
    final double[] mzs = new double[numDp];
    final double[] intensities = new double[numDp];

    StorageUtils.putAllValuesIntoOneArray(data, 0, mzs);
    final int[] rawBasePeakIndices = StorageUtils.putAllValuesIntoOneArray(data, 1, intensities);
    this.rawBasePeakIndices = StorageUtils.storeValuesToIntBuffer(storage, rawBasePeakIndices);

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

  /**
   * @param storage                The storage for mobility scans-
   * @param massDetector           The mass detector
   * @param massDetectorParameters The parameters for the mass detector.
   */
  public void generateAndAddMobilityScanMassLists(@Nullable MemoryMapStorage storage,
      @NotNull MassDetector massDetector, @NotNull ParameterSet massDetectorParameters) {

    // mobility scan -> [0][] = mzs, [1][] = intensities
    final List<double[][]> data = new ArrayList<>();

    for (MobilityScan mobilityScan : getMobilityScans()) {
      double[][] mzIntensity = massDetector.getMassValues(mobilityScan, massDetectorParameters);
      data.add(mzIntensity);
    }

    AtomicInteger biggestOffset = new AtomicInteger(0);
    final int[] massListStorageOffsets = StorageUtils.generateOffsets(data, biggestOffset);
    this.massListStorageOffsets = StorageUtils.storeValuesToIntBuffer(storage,
        massListStorageOffsets);
    massListMaxNumPoints = biggestOffset.get();

    final int numDp = massListStorageOffsets[massListStorageOffsets.length - 1] + data.get(
        data.size() - 1)[0].length;
    final double[] mzs = new double[numDp];
    final double[] intensities = new double[numDp];

    StorageUtils.putAllValuesIntoOneArray(data, 0, mzs);
    final int[] massListBasePeakIndices = StorageUtils.putAllValuesIntoOneArray(data, 1,
        intensities);
    this.massListBasePeakIndices = StorageUtils.storeValuesToIntBuffer(storage,
        massListBasePeakIndices);
    massListMzValues = StorageUtils.storeValuesToDoubleBuffer(storage, mzs);
    massListIntensityValues = StorageUtils.storeValuesToDoubleBuffer(storage, intensities);
  }

  public MassList getMassList(int mobilityScanIndex) {
    if (massListIntensityValues == null) {
      return null;
    }
    return new StoredMobilityScanMassList(mobilityScanIndex, this);
  }

  /**
   * Creates a {@link StoredMobilityScan} wrapper for the given index. Reuse when possible for ram
   * efficiency.
   *
   * @param index The scan index.
   * @return The mobility scan.
   */
  public MobilityScan getMobilityScan(int index) {
    assert index <= getNumberOfMobilityScans();
    return new StoredMobilityScan(index, this);
  }

  /**
   * Creates {@link StoredMobilityScan} wrappers for all mobility scans of this file. It's
   * recommended to not save the full array list for processing purposes, since the mobility scans
   * are created on every call to this method. E.g. during feature detection (for the purpose of
   * saving EICs/mobilogram trace) the SAME instance of {@link MobilityScan} should be used and
   * reused (see {@link SimpleIonMobilogramTimeSeries#getSpectraModifiable()}) at all times. For
   * example the mobility scans shall not be accessed via a {@link Frame} every time but retrieved
   * once and reused.
   *
   * @return The mobility scans.
   */
  public List<MobilityScan> getMobilityScans() {
    List<MobilityScan> scans = new ArrayList<>(getNumberOfMobilityScans());
    for (int i = 0; i < getNumberOfMobilityScans(); i++) {
      scans.add(new StoredMobilityScan(i, this));
    }
    return scans;
  }

  public int getNumberOfMobilityScans() {
    return rawStorageOffsets.capacity();
  }

  /**
   * @param index The mobility scan index.
   * @return The number of points in the mobility scan.
   */
  public int getNumberOfRawDatapoints(int index) {
    assert index < getNumberOfMobilityScans();
    if (index < rawStorageOffsets.capacity() - 1) {
      return rawStorageOffsets.get(index + 1) - rawStorageOffsets.get(index);
    } else {
      return rawMzValues.capacity() - rawStorageOffsets.get(index);
    }
  }

  /**
   * @param index The mobility scan index.
   * @return The base peak index or -1 if no base peak was found (scan empty).
   */
  public int getRawBasePeakIndex(int index) {
    assert index < getNumberOfMobilityScans();
    return rawBasePeakIndices.get(index);
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
    return rawStorageOffsets.get(mobilityScanIndex);
  }

  public void getRawMobilityScanMzValues(int mobilityScanIndex, double[] dst, int offset) {
    assert getNumberOfRawDatapoints(mobilityScanIndex) + offset <= dst.length;
    rawMzValues.get(getRawStorageOffset(mobilityScanIndex), dst, offset,
        getNumberOfRawDatapoints(mobilityScanIndex));
  }

  public void getAllRawMobilityScanMzValues(double[] dst) {
    assert dst.length >= getRawTotalNumPoints();
    rawMzValues.get(0, dst, 0, getRawTotalNumPoints());
  }

  public void getRawMobilityScanIntensityValues(int mobilityScanIndex, double[] dst, int offset) {
    assert getNumberOfRawDatapoints(mobilityScanIndex) + offset <= dst.length;
    rawIntensityValues.get(getRawStorageOffset(mobilityScanIndex), dst, offset,
        getNumberOfRawDatapoints(mobilityScanIndex));
  }

  public void getAllRawMobilityScanIntensityValues(double[] dst) {
    assert dst.length >= getRawTotalNumPoints();
    rawIntensityValues.get(0, dst, 0, getRawTotalNumPoints());
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
    if (index < massListStorageOffsets.capacity() - 1) {
      return massListStorageOffsets.get(index + 1) - massListStorageOffsets.get(index);
    } else {
      return massListMzValues.capacity() - massListStorageOffsets.get(index);
    }
  }

  /**
   * @param index The index of the mobility scan the mass list belongs to.
   * @return The storage offset (where data points of this mass list start)
   */
  public int getMassListStorageOffset(int index) {
    return massListStorageOffsets.get(index);
  }

  /**
   * @param index The index of the mobility scan this mass list belongs to.
   * @return The base peak index (may be -1 if no base peak was detected).
   */
  public int getMassListBasePeakIndex(int index) {
    return massListBasePeakIndices.get(index);
  }

  /**
   * @return The maximum number of points in a mass list.
   */
  public int getMassListMaxNumPoints() {
    return massListMaxNumPoints;
  }

  /**
   * @return The total number of data points in all mobility scan-mass lists of this frame.
   */
  public int getMassListTotalNumPoints() {
    return massListIntensityValues.capacity();
  }

  public void getMassListMzValues(int mobilityScanIndex, double[] dst, int offset) {
    assert getNumberOfMassListDatapoints(mobilityScanIndex) + offset <= dst.length;
    massListMzValues.get(getMassListStorageOffset(mobilityScanIndex), dst, offset,
        getNumberOfMassListDatapoints(mobilityScanIndex));
  }

  public void getAllMassListMzValues(double[] dst) {
    assert dst.length >= getMassListTotalNumPoints();
    massListMzValues.get(0, dst, 0, getMassListTotalNumPoints());
  }

  public void getMassListIntensityValues(int mobilityScanIndex, double[] dst, int offset) {
    assert getNumberOfMassListDatapoints(mobilityScanIndex) + offset <= dst.length;
    massListIntensityValues.get(getMassListStorageOffset(mobilityScanIndex), dst, offset,
        getNumberOfMassListDatapoints(mobilityScanIndex));
  }

  public void getAllMassListIntensityValues(double[] dst) {
    assert dst.length >= getMassListTotalNumPoints();
    massListIntensityValues.get(0, dst, 0, getMassListTotalNumPoints());
  }

  public double getMassListMzValue(int mobilityScanIndex, int index) {
    return massListMzValues.get(getMassListStorageOffset(mobilityScanIndex) + index);
  }

  public double getMassListIntensityValue(int mobilityScanIndex, int index) {
    return massListIntensityValues.get(getMassListStorageOffset(mobilityScanIndex) + index);
  }
}
