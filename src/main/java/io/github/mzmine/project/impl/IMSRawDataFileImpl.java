/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.project.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalibration;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author https://github.com/SteffenHeu
 * @see io.github.mzmine.datamodel.IMSRawDataFile
 */
public class IMSRawDataFileImpl extends RawDataFileImpl implements IMSRawDataFile {

  public static final String SAVE_IDENTIFIER = "Ion mobility Raw data file";
  private static Logger logger = Logger.getLogger(IMSRawDataFileImpl.class.getName());

  private final List<Frame> frames = FXCollections.observableArrayList();
  private final Hashtable<Integer, List<Scan>> frameNumbersCache;
  private final Hashtable<Integer, Range<Double>> dataMobilityRangeCache;
  private final Hashtable<Integer, List<Frame>> frameMsLevelCache;

  /**
   * Mobility <-> sub spectrum number is the same for a segment but might change between segments!
   * Key = Range of Frame numbers in a segment (inclusive) Value = Mapping of sub spectrum number ->
   * mobility
   */
  private final Map<Range<Integer>, Map<Integer, Double>> segmentMobilityRange;

  protected Range<Double> mobilityRange;
  protected MobilityType mobilityType;
  protected CCSCalibration ccsCalibration = null;

  public IMSRawDataFileImpl(String dataFileName, @Nullable final String absolutePath,
      MemoryMapStorage storage) throws IOException {
    super(dataFileName, absolutePath, storage);

    frameNumbersCache = new Hashtable<>();
    dataMobilityRangeCache = new Hashtable<>();
    frameMsLevelCache = new Hashtable<>();
    segmentMobilityRange = new HashMap<>();

    mobilityRange = null;
    mobilityType = MobilityType.NONE;

  }

  public IMSRawDataFileImpl(String dataFileName, @Nullable final String absolutePath,
      MemoryMapStorage storage, Color color) throws IOException {
    super(dataFileName, absolutePath, storage, color);

    frameNumbersCache = new Hashtable<>();
    dataMobilityRangeCache = new Hashtable<>();
    frameMsLevelCache = new Hashtable<>();
    segmentMobilityRange = new HashMap<>();

    mobilityRange = null;
    mobilityType = MobilityType.NONE;
  }

  @Override
  public synchronized void addScan(Scan newScan) throws IOException {

    if (!(newScan instanceof Frame)) {
      throw new UnsupportedOperationException("Cannot add " + newScan.getClass().getName()
          + ". Only instances of Frame can be added to an IMSRawDataFile");
    }
    super.addScan(newScan);

    Frame newFrame = (Frame) newScan;
    // TODO: dirty hack - currently the frames are added to the scan and frame map
    if (this.mobilityType == MobilityType.NONE) {
      this.mobilityType = newFrame.getMobilityType();
    }
    if (newFrame.getMobilityType() != mobilityType) {
      throw new UnsupportedOperationException(
          "The mobility type specified in scan (" + newFrame.getMobilityType()
              + ") does not match the mobility type of raw data file (" + getMobilityType() + ")");
    }

//    Range<Integer> segmentKey = getSegmentKeyForFrame((newFrame).getScanNumber());
//    segmentMobilityRange.putIfAbsent(segmentKey, newFrame.getMobilities());

    frames.add(newFrame);
    /*
     * if (mobilityRange == null) { mobilityRange = Range.singleton(newScan.getMobility()); } else
     * if (!mobilityRange.contains(newScan.getMobility())) { mobilityRange =
     * mobilityRange.span(Range.singleton(newScan.getMobility())); } super.addScan(newScan);
     */
  }

  @NotNull
  @Override
  public List<Frame> getFrames() {
    return frames;
  }

  @NotNull
  @Override
  public List<Frame> getFrames(int msLevel) {
    return frameMsLevelCache.computeIfAbsent(msLevel,
        level -> getFrames().stream().filter(frame -> frame.getMSLevel() == msLevel).toList());
  }

  @Nullable
  @Override
  public Frame getFrame(int frameNum) {
    return frames.get(frameNum);
  }

  @Override
  @NotNull
  public List<Frame> getFrames(int msLevel, Range<Float> rtRange) {
    return getFrames(msLevel).stream().filter(frame -> rtRange.contains(frame.getRetentionTime()))
        .toList();
  }

  @NotNull
  @Override
  public List<Scan> getFrameNumbers(int msLevel) {
    return frameNumbersCache.computeIfAbsent(msLevel, (key) -> {
      List<Scan> frameNums = new ArrayList<>();
      synchronized (frames) {
        for (Scan e : frames) {
          if (e.getMSLevel() == msLevel) {
            frameNums.add(e);
          }
        }
      }
      return frameNums;
    });
  }

  @Override
  public int getNumberOfFrames() {
    return frames.size();
  }

  @NotNull
  @Override
  public List<Scan> getFrameNumbers(int msLevel, @NotNull Range<Float> rtRange) {
    // since {@link getFrameNumbers(int)} is prefiltered, this shouldn't lead to NPE
    return getFrameNumbers(msLevel).stream()
        .filter(frameNum -> rtRange.contains(frameNum.getRetentionTime())).toList();
  }

  @NotNull
  @Override
  public Range<Double> getDataMobilityRange() {
    mobilityRange = dataMobilityRangeCache.computeIfAbsent(0, level -> {
      double lower = 1E10;
      double upper = -1E10;
      synchronized (frames) {
        for (Frame e : getFrames()) {
          if (e.getMobilityRange().lowerEndpoint() < lower) {
            lower = e.getMobilityRange().lowerEndpoint();
          }
          if (e.getMobilityRange().upperEndpoint() > upper) {
            upper = e.getMobilityRange().upperEndpoint();
          }
        }
      }
      return Range.closed(lower, upper);
    });

    return mobilityRange;
  }

  @Override
  @Nullable
  public Frame getFrameAtRt(double rt) {
    if (rt > getDataRTRange().upperEndpoint()) {
      return null;
    }

    List<Frame> frameList = getFrames().stream().sorted(Comparator.comparingInt(Frame::getFrameId))
        .collect(Collectors.toList());
    double minDiff = 10E10;

    for (int i = 0; i < frameList.size(); i++) {
      double diff = Math.abs(rt - frameList.get(i).getRetentionTime());
      if (diff < minDiff) {
        minDiff = diff;
      } else if (diff > minDiff) { // not triggered in first run
        return frameList.get(i - 1); // the previous one was better
      }
    }
    return null;
  }

  @Nullable
  @Override
  public Frame getFrameAtRt(double rt, int msLevel) {
    if (rt > getDataRTRange(msLevel).upperEndpoint()) {
      return null;
    }
    Range<Float> range = Range.closed((float) rt - 2, (float) rt + 2);
    List<Frame> eligibleFrames = getFrames(msLevel, range);
    double minDiff = 10E6;

    for (int i = 0; i < eligibleFrames.size(); i++) {
      double diff = Math.abs(rt - eligibleFrames.get(i).getRetentionTime());
      if (diff < minDiff) {
        minDiff = diff;
      } else if (diff > minDiff) { // not triggered in first run
        return eligibleFrames.get(i - 1); // the previous one was better
      }
    }
    return null;
  }

  @NotNull
  @Override
  public MobilityType getMobilityType() {
    return mobilityType;
  }

  @NotNull
  @Override
  public Range<Double> getDataMobilityRange(int msLevel) {
    if (dataMobilityRangeCache.get(msLevel) == null) {
      double lower = 1E10;
      double upper = -1E10;
      synchronized (frames) {
        for (Frame e : getFrames()) {
          if (e.getMSLevel() == msLevel && e.getMobilityRange().lowerEndpoint() < lower) {
            lower = e.getMobilityRange().lowerEndpoint();
          }
          if (e.getMSLevel() == msLevel && e.getMobilityRange().upperEndpoint() > upper) {
            upper = e.getMobilityRange().upperEndpoint();
          }
        }
      }
      dataMobilityRangeCache.put(msLevel, Range.closed(lower, upper));
    }
    return dataMobilityRangeCache.get(msLevel);
  }

  /**
   * @param frameRange The range (in frame ids) for an acquisition segment.
   */
  public void addSegment(Range<Integer> frameRange) {
    segmentMobilityRange.put(frameRange, null);
  }

  /**
   * @param frameNumber The frame number
   * @param mobilitySpectrumNumber The mobility spectrum number with regard to the frame.
   * @return The mobility for the respective scan or {@link MobilityScan#DEFAULT_MOBILITY}.
   */
  /*@Override
  public double getMobilityForMobilitySpectrum(int frameNumber, int mobilitySpectrumNumber) {
    Map<Integer, Double> mobilities = getMobilitiesForFrame(frameNumber);
    if (mobilities != null) {
      return mobilities.getOrDefault(mobilitySpectrumNumber, MobilityScan.DEFAULT_MOBILITY);
    }
    return MobilityScan.DEFAULT_MOBILITY;
  }*/

  /**
   * @param frameId The frame number.
   * @return Map of mobility scan number <-> mobility or null for invalid frame numbers.
   */
  @Nullable
  /*@Override
  public Map<Integer, Double> getMobilitiesForFrame(int frameNumber) {
    Optional<Entry<Range<Integer>, Map<Integer, Double>>> entry = segmentMobilityRange.entrySet()
        .stream().filter(e -> e.getKey().contains(frameNumber)).findFirst();
    return entry.map(Entry::getValue).orElse(null);
  }*/

  private Range<Integer> getSegmentKeyForFrame(int frameId) {
    return segmentMobilityRange.keySet().stream()
        .filter(segmentRange -> segmentRange.contains(frameId)).findFirst().get();
  }

  /**
   * Method to check if the proposed number of datapoints exceeds the current max number of
   * datapoints. Used in case the data points of a frame are altered. E.g. when a MZML IMS file is
   * imported. At that point, no summed frame is available and will have to be created later on.
   *
   * @param proposedValue The proposed number of data points.
   */
  public void updateMaxRawDataPoints(int proposedValue) {
    if (proposedValue > getMaxRawDataPoints()) {
      maxRawDataPoints = proposedValue;
    }
  }

  @Override
  public @Nullable CCSCalibration getCCSCalibration() {
    return ccsCalibration;
  }

  @Override
  public void setCCSCalibration(@Nullable CCSCalibration calibration) {
    ccsCalibration = calibration;
  }
}
