/*
 * Copyright (c) 2004-2024 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.impl;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.datamodel.impl.masslist.StoredMobilityScanMassList;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanUtils;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Memory efficient storage of {@link MobilityScan}s. Methods return an instance of
 * {@link StoredMobilityScan} or {@link StoredMobilityScanMassList} which is garbage collected if
 * not used anymore.
 *
 * @author https://github.com/steffenheu
 */
public class MobilityScanStorage {

  // raw data
  @NotNull
  private final Frame frame;
  /**
   * doubles
   */
  private final MemorySegment rawMzValues;
  /**
   * doubles
   */
  private final MemorySegment rawIntensityValues;
  /**
   * Per scan, ints.
   */
  private final MemorySegment rawStorageOffsets;
  /**
   * Per scan, ints.
   */
  private final MemorySegment rawBasePeakIndices;
  private final int rawMaxNumPoints;

  // mass list
  /**
   * doubles
   */
  private MemorySegment massListMzValues = null;
  /**
   * doubles
   */
  private MemorySegment massListIntensityValues = null;
  /**
   * Per scan, ints.
   */
  private MemorySegment massListStorageOffsets = null;
  /**
   * Per scan, ints.
   */
  private MemorySegment massListBasePeakIndices = null;
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
   * Costructor with preloaded already memory mapped data. This is used in the mzML
   *
   * @param storage         for memory mapping
   * @param frame           the actual frame
   * @param mzValues        already memory mapped mz values, doubles.
   * @param intensityValues already memory mapped intensity values, doubles.
   * @param maxNumPoints    the maximum number of signals in the largest scan
   * @param storageOffsets  the offsets to find the start of each mobility scan in the memory mapped
   *                        mz and intensity buffers
   * @param basePeakIndices the
   * @param useAsMassList
   */
  public MobilityScanStorage(final @Nullable MemoryMapStorage storage, final SimpleFrame frame,
      final MemorySegment mzValues, final MemorySegment intensityValues, final int maxNumPoints,
      final int[] storageOffsets, final int[] basePeakIndices, final boolean useAsMassList) {
    this.frame = frame;
    rawBasePeakIndices = StorageUtils.storeValuesToIntBuffer(storage, basePeakIndices);
    rawStorageOffsets = StorageUtils.storeValuesToIntBuffer(storage, storageOffsets);
    rawMzValues = mzValues;
    rawIntensityValues = intensityValues;
    rawMaxNumPoints = maxNumPoints;

    if (useAsMassList) {
      massListBasePeakIndices = rawBasePeakIndices;
      massListMaxNumPoints = rawMaxNumPoints;
      massListMzValues = rawMzValues;
      massListIntensityValues = rawIntensityValues;
      massListStorageOffsets = rawStorageOffsets;
    }
  }

  /**
   * @param storage      The storage for mobility scans-
   * @param massDetector The mass detector
   */
  public void generateAndAddMobilityScanMassLists(@Nullable MemoryMapStorage storage,
      @NotNull MassDetector massDetector, boolean denormalizeMSnScans) {

    if (!massDetector.filtersActive()) {
      // no need to run mass detection in this case.
      massListBasePeakIndices = rawBasePeakIndices;
      massListMaxNumPoints = rawMaxNumPoints;
      massListMzValues = rawMzValues;
      massListIntensityValues = rawIntensityValues;
      massListStorageOffsets = rawStorageOffsets;
      return;
    }

    // mobility scan -> [0][] = mzs, [1][] = intensities
    final List<double[][]> data = new ArrayList<>();

    for (MobilityScan mobilityScan : getMobilityScans()) {
      double[][] mzIntensity = massDetector.getMassValues(mobilityScan);
      if (denormalizeMSnScans && frame.getMSLevel() > 1) {
        ScanUtils.denormalizeIntensitiesMultiplyByInjectTime(mzIntensity[1],
            frame.getInjectionTime());
      }
      data.add(mzIntensity);
    }

    setMassLists(storage, data);
  }

  /**
   * Sets the new masslists
   *
   * @param storage memory storage for masslists
   * @param data    the masslists as [0,1] as [mzs, intensities] arrays, one for each MobilityScan
   *                in this frame
   */
  public void setMassLists(final @Nullable MemoryMapStorage storage, final List<double[][]> data) {
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
    return (int) StorageUtils.numInts(rawStorageOffsets);
  }

  /**
   * @param index The mobility scan index.
   * @return The number of points in the mobility scan.
   */
  public int getNumberOfRawDatapoints(int index) {
    assert index < getNumberOfMobilityScans();
    if (index < getNumberOfMobilityScans() - 1) {
      return getRawStorageOffset(index + 1) - getRawStorageOffset(index);
    } else {
      return (int) (getRawTotalNumPoints() - getRawStorageOffset(index));
    }
  }

  /**
   * @param index The mobility scan index.
   * @return The base peak index or -1 if no base peak was found (scan empty).
   */
  public int getRawBasePeakIndex(int index) {
    assert index < getNumberOfMobilityScans();
    return rawBasePeakIndices.getAtIndex(ValueLayout.JAVA_INT, index);
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
    return (int) StorageUtils.numDoubles(rawMzValues);
  }

  public Frame getFrame() {
    return frame;
  }

  public int getRawStorageOffset(int mobilityScanIndex) {
    assert mobilityScanIndex < getNumberOfMobilityScans();
    return rawStorageOffsets.getAtIndex(ValueLayout.JAVA_INT, mobilityScanIndex);
  }

  /**
   *
   */
  public void getRawMobilityScanMzValues(int mobilityScanIndex, double[] dst) {
    final int rawNumDp = getNumberOfRawDatapoints(mobilityScanIndex);
    assert rawNumDp <= dst.length;
    final MemorySegment slice = StorageUtils.sliceDoubles(rawMzValues,
        getRawStorageOffset(mobilityScanIndex), getRawStorageOffset(mobilityScanIndex) + rawNumDp);
    MemorySegment.copy(slice, ValueLayout.JAVA_DOUBLE, 0, dst, 0, rawNumDp);
  }

  public void getAllRawMobilityScanMzValues(double[] dst) {
    assert dst.length >= getRawTotalNumPoints();
    StorageUtils.copyToBuffer(dst, rawMzValues, 0, getRawTotalNumPoints());
  }

  /**
   *
   */
  public void getRawMobilityScanIntensityValues(int mobilityScanIndex, double[] dst) {
    final int rawNumDp = getNumberOfRawDatapoints(mobilityScanIndex);
    assert rawNumDp <= dst.length;

    final MemorySegment slice = StorageUtils.sliceDoubles(rawIntensityValues,
        getRawStorageOffset(mobilityScanIndex), getRawStorageOffset(mobilityScanIndex) + rawNumDp);
    MemorySegment.copy(slice, ValueLayout.JAVA_DOUBLE, 0, dst, 0, rawNumDp);
  }

  public void getAllRawMobilityScanIntensityValues(double[] dst) {
    assert dst.length >= getRawTotalNumPoints();
    StorageUtils.copyToBuffer(dst, rawIntensityValues, 0, getRawTotalNumPoints());
  }

  public double getRawMobilityScanMzValue(int mobilityScanIndex, int index) {
    return rawMzValues.getAtIndex(ValueLayout.JAVA_DOUBLE,
        getRawStorageOffset(mobilityScanIndex) + index);
  }

  public double getRawMobilityScanIntensityValue(int mobilityScanIndex, int index) {
    return rawIntensityValues.getAtIndex(ValueLayout.JAVA_DOUBLE,
        getRawStorageOffset(mobilityScanIndex) + index);
  }

  // mass list
  public int getNumberOfMassListDatapoints(int index) {
    if (massListStorageOffsets == null) {
      throw new MissingMassListException(
          "No mass list present for mobility scans. Run mass detection for scan type \"Mobility scans\" prior.",
          frame);
    }
    assert index < getNumberOfMobilityScans();

    if (index < getNumberOfMobilityScans() - 1) {
      return getMassListStorageOffset(index + 1) - getMassListStorageOffset(index);
    } else {
      return getMassListTotalNumPoints() - getMassListStorageOffset(index);
    }
  }

  /**
   * @param index The index of the mobility scan the mass list belongs to.
   * @return The storage offset (where data points of this mass list start)
   */
  public int getMassListStorageOffset(int index) {
    if (massListStorageOffsets == null) {
      throw new MissingMassListException(
          "No mass list present for mobility scans. Run mass detection for scan type \"Mobility scans\" prior.",
          frame);
    }
    return massListStorageOffsets.getAtIndex(ValueLayout.JAVA_INT, index);
  }

  /**
   * @param index The index of the mobility scan this mass list belongs to.
   * @return The base peak index (may be -1 if no base peak was detected).
   */
  public int getMassListBasePeakIndex(int index) {
    if (massListBasePeakIndices == null) {
      throw new MissingMassListException(
          "No mass list present for mobility scans. Run mass detection for scan type \"Mobility scans\" prior.",
          frame);
    }
    return massListBasePeakIndices.getAtIndex(ValueLayout.JAVA_INT, index);
  }

  /**
   * @return The maximum number of points in a mass list.
   */
  public int getMassListMaxNumPoints() {
    if (massListMaxNumPoints == -1) {
      throw new MissingMassListException(
          "No mass list present for mobility scans. Run mass detection for scan type \"Mobility scans\" prior.",
          frame);
    }
    return massListMaxNumPoints;
  }

  /**
   * @return The total number of data points in all mobility scan-mass lists of this frame.
   */
  public int getMassListTotalNumPoints() {
    if (massListIntensityValues == null) {
      throw new MissingMassListException(
          "No mass list present for mobility scans. Run mass detection for scan type \"Mobility scans\" prior.",
          frame);
    }
    return (int) StorageUtils.numDoubles(massListIntensityValues);
  }

  public void getMassListMzValues(int mobilityScanIndex, double[] dst) {
    if (massListMzValues == null) {
      throw new MissingMassListException(
          "No mass list present for mobility scans. Run mass detection for scan type \"Mobility scans\" prior.",
          frame);
    }
    final int numMassListDp = getNumberOfMassListDatapoints(mobilityScanIndex);
    assert numMassListDp <= dst.length;

    final MemorySegment slice = StorageUtils.sliceDoubles(massListMzValues,
        getMassListStorageOffset(mobilityScanIndex),
        getMassListStorageOffset(mobilityScanIndex) + numMassListDp);
    MemorySegment.copy(slice, ValueLayout.JAVA_DOUBLE, 0, dst, 0, numMassListDp);
  }

  public void getAllMassListMzValues(double[] dst) {
    if (massListMzValues == null) {
      throw new MissingMassListException(
          "No mass list present for mobility scans. Run mass detection for scan type \"Mobility scans\" prior.",
          frame);
    }
    assert dst.length >= getMassListTotalNumPoints();
    StorageUtils.copyToBuffer(dst, massListMzValues, 0, getMassListTotalNumPoints());
  }

  public void getMassListIntensityValues(int mobilityScanIndex, double[] dst) {
    if (massListIntensityValues == null) {
      throw new MissingMassListException(
          "No mass list present for mobility scans. Run mass detection for scan type \"Mobility scans\" prior.",
          frame);
    }
    final int numMassListDp = getNumberOfMassListDatapoints(mobilityScanIndex);
    assert numMassListDp <= dst.length;

    final MemorySegment slice = StorageUtils.sliceDoubles(massListIntensityValues,
        getMassListStorageOffset(mobilityScanIndex),
        getMassListStorageOffset(mobilityScanIndex) + numMassListDp);
    MemorySegment.copy(slice, ValueLayout.JAVA_DOUBLE, 0, dst, 0, numMassListDp);
  }

  public void getAllMassListIntensityValues(double[] dst) {
    if (massListIntensityValues == null) {
      throw new MissingMassListException(
          "No mass list present for mobility scans. Run mass detection for scan type \"Mobility scans\" prior.",
          frame);
    }
    assert dst.length >= getMassListTotalNumPoints();
    StorageUtils.copyToBuffer(dst, massListIntensityValues, 0, getMassListTotalNumPoints());
  }

  public double getMassListMzValue(int mobilityScanIndex, int index) {
    if (massListMzValues == null) {
      throw new MissingMassListException(
          "No mass list present for mobility scans. Run mass detection for scan type \"Mobility scans\" prior.",
          frame);
    }
    return massListMzValues.getAtIndex(ValueLayout.JAVA_DOUBLE,
        getMassListStorageOffset(mobilityScanIndex) + index);
  }

  public double getMassListIntensityValue(int mobilityScanIndex, int index) {
    if (massListIntensityValues == null) {
      throw new MissingMassListException(
          "No mass list present for mobility scans. Run mass detection for scan type \"Mobility scans\" prior.",
          frame);
    }
    return massListIntensityValues.getAtIndex(ValueLayout.JAVA_DOUBLE,
        getMassListStorageOffset(mobilityScanIndex) + index);
  }
}
