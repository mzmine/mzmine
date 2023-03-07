/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel.data_access;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.impl.MobilityScanStorage;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.exceptions.MissingMassListException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MobilityScanDataAccess implements MobilityScan {

  protected final IMSRawDataFile dataFile;
  protected final MobilityScanDataType type;
  protected final int totalFrames;

  protected final List<Frame> eligibleFrames;
  private final ScanSelection selection;
  protected final double[] mzs;
  protected final double[] intensities;
  protected final Map<Frame, Integer> frameIndexMap = new HashMap<>();
  // current data
  protected Frame currentFrame;
  protected MobilityScan currentMobilityScan;
  protected MassSpectrum currentSpectrum;
  protected List<MobilityScan> currentMobilityScans;
  protected int currentNumberOfDataPoints = -1;
  protected int currentNumberOfMobilityScans = -1;
  protected int currentMobilityScanIndex = -1;
  protected int currentFrameIndex = -1;
  protected int currentSpectrumDatapointIndexOffset = 0;

  /**
   * The intended use of this memory access is to loop over all scans and access data points via
   * {@link #getMzValue(int)} and {@link #getIntensityValue(int)}
   *
   * @param dataFile  target data file to loop over all scans or mass lists
   * @param type      processed or raw data
   * @param selection processed or raw data
   */
  protected MobilityScanDataAccess(IMSRawDataFile dataFile, MobilityScanDataType type,
      ScanSelection selection) {
    this(dataFile, type, (List<Frame>) selection.getMatchingScans(dataFile.getFrames()), selection);
  }

  public MobilityScanDataAccess(@NotNull final IMSRawDataFile dataFile,
      @NotNull final MobilityScanDataType type, @NotNull final List<Frame> frames) {
    this(dataFile, type, frames, null);
  }

  public MobilityScanDataAccess(@NotNull final IMSRawDataFile dataFile,
      @NotNull final MobilityScanDataType type, @NotNull final List<Frame> frames,
      ScanSelection selection) {
    this.dataFile = dataFile;
    this.type = type;

    // count matching scans
    eligibleFrames = frames;
    this.selection = selection;
    totalFrames = eligibleFrames.size();

    final int length = getMaxNumberOfDataPoints(eligibleFrames);
    mzs = new double[length];
    intensities = new double[length];
  }

  /**
   * @return Number of data points in the current scan depending of the defined DataType
   * (RAW/CENTROID)
   */
  @Override
  public int getNumberOfDataPoints() {
    return currentNumberOfDataPoints;
  }

  public MobilityScan getCurrentMobilityScan() {
    return currentMobilityScan;
  }

  @NotNull
  @Override
  public RawDataFile getDataFile() {
    return dataFile;
  }

  @Override
  public double getMobility() {
    return currentMobilityScan.getMobility();
  }

  @Override
  public MobilityType getMobilityType() {
    return currentFrame.getMobilityType();
  }

  /**
   * @return The current frame.
   */
  @Override
  public Frame getFrame() {
    return currentFrame;
  }

  @Override
  public float getRetentionTime() {
    return currentFrame.getRetentionTime();
  }

  @Override
  public int getMobilityScanNumber() {
    return currentMobilityScan.getMobilityScanNumber();
  }

  @Nullable
  @Override
  public MsMsInfo getMsMsInfo() {
    return currentMobilityScan.getMsMsInfo();
  }

  @Override
  public void addMassList(@NotNull MassList massList) {
    throw new UnsupportedOperationException("Cannot set a mass list for a MobilityScanDataAccess.");
  }

  public boolean hasNextMobilityScan() {
    final int nextNum = currentMobilityScanIndex + 1;
    if (currentFrame == null || nextNum >= currentNumberOfMobilityScans) {
      return false;
    }

    if (selection != null) {
      return selection.matches(currentFrame.getMobilityScan(nextNum));
    }
    return true;
  }

  /**
   * Set the data to the next scan, if available. Returns the scan for additional data access. m/z
   * and intensity values should be accessed from this data class via {@link #getMzValue(int)} and
   * {@link #getIntensityValue(int)}
   *
   * @return the scan or null
   * @throws MissingMassListException if DataType.CENTROID is selected and mass list is missing in
   *                                  the current scan
   */
  public MobilityScan nextMobilityScan() throws MissingMassListException {
    if (!hasNextMobilityScan()) {
      return null;
    }

    currentMobilityScanIndex++;
    if (currentSpectrum != null) {
      // increment by the last mobility scan!
      currentSpectrumDatapointIndexOffset += currentSpectrum.getNumberOfDataPoints();
    }

    // set new mobility scan after incrementing.
    currentMobilityScan = currentMobilityScans.get(currentMobilityScanIndex);
    currentSpectrum =
        type == MobilityScanDataType.RAW ? currentMobilityScan : currentMobilityScan.getMassList();

    currentNumberOfDataPoints = currentSpectrum.getNumberOfDataPoints();
    if (currentSpectrumDatapointIndexOffset + currentNumberOfDataPoints > mzs.length) {
      throw new IndexOutOfBoundsException(
          "currentSpectrumDatapointIndexOffset + currentNumberOfDataPoints > mzs.length");
    }

    return currentMobilityScan;
  }

  public void resetMobilityScan() {
    currentMobilityScanIndex = -1;
    currentMobilityScan = null;
    currentSpectrum = null;
    currentNumberOfDataPoints = 0;
    currentSpectrumDatapointIndexOffset = 0;
  }


  public boolean hasNextFrame() {
    return currentFrameIndex + 1 < totalFrames;
  }

  /**
   * Sets the next frame. The mobility scan index is reset to -1, therefore
   * {@link #nextMobilityScan} has to be called before accessing new scan data.
   *
   * @return the next Frame.
   */
  public Frame nextFrame() {
    if (!hasNextFrame()) {
      return null;
    }

    currentFrameIndex++;
    currentFrame = eligibleFrames.get(currentFrameIndex);
    currentNumberOfMobilityScans = currentFrame.getNumberOfMobilityScans();
    currentMobilityScanIndex = -1;
    currentMobilityScan = null;
    currentSpectrum = null;

    currentMobilityScans = currentFrame.getMobilityScans();

    currentSpectrumDatapointIndexOffset = 0;

    if (selection != null) {
      for (int i = 0; i < currentMobilityScans.size(); i++) {
        MobilityScan tmpMobScan = currentMobilityScans.get(i);
        if (selection.matches(tmpMobScan)) {
          break;
        }

        currentMobilityScanIndex = i;
        MassSpectrum currentSpec =
            type == MobilityScanDataType.RAW ? tmpMobScan : tmpMobScan.getMassList();
        currentSpectrumDatapointIndexOffset += currentSpec.getNumberOfDataPoints();
      }
    }

    final MobilityScanStorage storage = currentFrame.getMobilityScanStorage();
    if (type == MobilityScanDataType.RAW) {
      storage.getAllRawMobilityScanMzValues(mzs);
      storage.getAllRawMobilityScanIntensityValues(intensities);
    } else {
      storage.getAllMassListMzValues(mzs);
      storage.getAllMassListIntensityValues(intensities);
    }

    return currentFrame;
  }

  /**
   * Resets the {@link MobilityScanDataAccess} to the initial state equal to the initialisation.
   */
  public void resetFrame() {
    currentFrameIndex = -1;
    currentFrame = null;
    currentNumberOfMobilityScans = -1;
    resetMobilityScan();
  }

  public void jumpToFrame(Frame frame) {
    jumpToFrameIndex(indexOfFrame(frame));
  }

  public void jumpToFrameIndex(int index) {
    if (index <= -1 || index >= eligibleFrames.size()) {
      throw new IllegalArgumentException("Illegal index " + index);
    }

    currentFrameIndex = index - 1;
    nextFrame();
  }

  public int indexOfFrame(Frame frame) {
    if (frameIndexMap.isEmpty()) {
      int index = 0;
      for (Frame eligibleFrame : eligibleFrames) {
        final var val = frameIndexMap.put(eligibleFrame, index);
        if (val != null) {
          throw new IllegalStateException("Clash of Frame hash codes.");
        }
        index++;
      }
    }
    final Integer index = frameIndexMap.get(frame);
    return index != null ? index : -1;
  }

  public MobilityScan jumpToMobilityScan(MobilityScan scan) {
    jumpToFrame(scan.getFrame());
    MobilityScan mobilityScan = null;
    while (currentMobilityScanIndex < scan.getMobilityScanNumber()) {
      mobilityScan = nextMobilityScan();
    }
    return mobilityScan;
  }

  public MassList getMassList() {
    return currentSpectrum instanceof MassList ml ? ml : currentMobilityScan.getMassList();
  }

  /**
   * Get mass-to-charge ratio at index
   *
   * @param index data point index
   */
  @Override
  public double getMzValue(int index) {
    assert index < getNumberOfDataPoints() && index >= 0;
    assert currentSpectrumDatapointIndexOffset + index < mzs.length;
    return mzs[currentSpectrumDatapointIndexOffset + index];
  }

  /**
   * Get intensity at index
   *
   * @param index data point index
   */
  @Override
  public double getIntensityValue(int index) {
    assert index < getNumberOfDataPoints() && index >= 0;
    assert currentSpectrumDatapointIndexOffset + index < intensities.length;
    return intensities[currentSpectrumDatapointIndexOffset + index];
  }

  /**
   * Number of selected scans
   */
  public int getNumberOfScans() {
    return totalFrames;
  }

  /**
   * Maximum number of data points is used to create the arrays that back the data
   */
  private int getMaxNumberOfDataPoints(List<Frame> frames) {
    return switch (type) {
      case RAW ->
          frames.stream().mapToInt(Frame::getTotalMobilityScanRawDataPoints).max().orElse(0);
      case CENTROID ->
          frames.stream().mapToInt(Frame::getTotalMobilityScanMassListDataPoints).max().orElse(0);
    };
  }

  // ###############################################
  // general MassSpectrum methods

  @Override
  public MassSpectrumType getSpectrumType() {
    return switch (type) {
      case RAW -> currentFrame.getSpectrumType();
      case CENTROID -> MassSpectrumType.CENTROIDED;
    };
  }

  @Nullable
  @Override
  public Double getBasePeakMz() {
    Integer index = getBasePeakIndex();
    return index != null && index >= 0 ? getMzValue(index) : null;
  }

  @Nullable
  @Override
  public Double getBasePeakIntensity() {
    Integer index = getBasePeakIndex();
    return index != null && index >= 0 ? getIntensityValue(index) : null;
  }

  @Nullable
  @Override
  public Integer getBasePeakIndex() {
    switch (type) {
      case RAW:
        return getCurrentMobilityScan().getBasePeakIndex();
      case CENTROID:
        MassList masses = getMassList();
        return masses == null ? null : masses.getBasePeakIndex();
      default:
        throw new IllegalStateException("Unexpected value: " + type);
    }
  }

  @Nullable
  @Override
  public Range<Double> getDataPointMZRange() {
    switch (type) {
      case RAW:
        return getCurrentMobilityScan().getDataPointMZRange();
      case CENTROID:
        MassList masses = getMassList();
        return masses == null ? null : masses.getDataPointMZRange();
      default:
        throw new IllegalStateException("Unexpected value: " + type);
    }
  }

  public List<Frame> getEligibleFrames() {
    return eligibleFrames;
  }

  @Nullable
  @Override
  public Double getTIC() {
    return ArrayUtils.sum(intensities, 0, currentNumberOfDataPoints);
  }

  @Override
  public double[] getMzValues(@NotNull double[] dst) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all scans and data points");
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all scans and data points");
  }

  @NotNull
  @Override
  public Iterator<DataPoint> iterator() {
    throw new UnsupportedOperationException(
        "The intended use of this class is to loop over all scans and data points");
  }

  @Override
  public @Nullable Float getInjectionTime() {
    return currentMobilityScan.getInjectionTime();
  }
}
