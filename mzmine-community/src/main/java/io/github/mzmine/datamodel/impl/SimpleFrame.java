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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.msms.IonMobilityMsMsInfo;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import it.unimi.dsi.fastutil.doubles.DoubleImmutableList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author https://github.com/SteffenHeu
 * @see Frame
 */
public class SimpleFrame extends SimpleScan implements Frame {

  private static Logger logger = Logger.getLogger(SimpleFrame.class.getName());

  private final MobilityType mobilityType;

  @NotNull
  private Set<IonMobilityMsMsInfo> precursorInfos;
  private Range<Double> mobilityRange;

  private int mobilitySegment = -1;
  private MobilityScanStorage mobilityScanStorage;

  public SimpleFrame(@NotNull RawDataFile dataFile, int scanNumber, int msLevel,
      float retentionTime, @Nullable double[] mzValues, @Nullable double[] intensityValues,
      MassSpectrumType spectrumType, PolarityType polarity, String scanDefinition,
      @NotNull Range<Double> scanMZRange, MobilityType mobilityType,
      @Nullable Set<IonMobilityMsMsInfo> precursorInfos, Float accumulationTime) {
    super(dataFile, scanNumber, msLevel, retentionTime, null, /*
         * fragmentScans,
         */
        mzValues, intensityValues, spectrumType, polarity, scanDefinition, scanMZRange,
        accumulationTime);

    this.mobilityType = mobilityType;
    mobilityRange = Range.singleton(0.d);
    setPrecursorInfos(precursorInfos);
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
    setMobilityScanStorage(
        new MobilityScanStorage(getDataFile().getMemoryMapStorage(), this, originalMobilityScans,
            useAsMassList));
  }

  public void setMobilityScanStorage(final MobilityScanStorage mobilityScanStorage) {
    this.mobilityScanStorage = mobilityScanStorage;
  }

  @Override
  public double getMobilityForMobilityScanNumber(int mobilityScanIndex) {
    return ((IMSRawDataFile) (getDataFile())).getSegmentMobilities(mobilitySegment)
        .getDouble(mobilityScanIndex);
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
  public Set<IonMobilityMsMsInfo> getImsMsMsInfos() {
    return precursorInfos;
  }

  @Nullable
  @Override
  public IonMobilityMsMsInfo getImsMsMsInfoForMobilityScan(int mobilityScanNumber) {
    if (precursorInfos == null) {
      return null;
    }
    Optional<IonMobilityMsMsInfo> pcInfo = precursorInfos.stream()
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
    if (mobilities.length == 0) {
      logger.info(
          () -> String.format("No mobilities detected in frame #%d of file %s.", getFrameId(),
              getDataFile().getName()));
      mobilities = new double[]{1d};
      mobilityRange = Range.openClosed(1d, 1d); // empty range
    } else {
      mobilityRange = Range.singleton(mobilities[0]);
      mobilityRange = mobilityRange.span(Range.singleton(mobilities[mobilities.length - 1]));
    }
    mobilitySegment = ((IMSRawDataFile) getDataFile()).addMobilityValues(mobilities);
    return mobilitySegment;
  }

  public void setPrecursorInfos(@Nullable Set<IonMobilityMsMsInfo> precursorInfos) {
    // precursorInfos needs to be modifiable
    this.precursorInfos = precursorInfos != null ? precursorInfos : new HashSet<>(0);
    this.precursorInfos.forEach(i -> i.setMsMsScan(this));
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
