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

package io.github.mzmine.project.impl;

import com.google.common.collect.Range;
import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityMassSpectrum;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.MobilityDataPoint;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.Mobilogram;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IMSRawDataFileImpl extends RawDataFileImpl implements IMSRawDataFile {

  public static final String SAVE_IDENTIFIER = "Ion mobility Raw data file";
  private static Logger logger = Logger.getLogger(IMSRawDataFileImpl.class.getName());
  protected final MobilityDataPointStorage mdpStorage;
  private final TreeMap<Integer, StorableFrame> frames;
  private final Hashtable<Integer, Set<Integer>> frameNumbersCache;
  private final Hashtable<Integer, Range<Double>> dataMobilityRangeCache;
  private final Hashtable<Integer, Collection<? extends Frame>> frameMsLevelCache;

  /**
   * Mobility <-> sub spectrum number is the same for a segment but might change between segments!
   * Key = Range of Frame numbers in a segment (inclusive) Value = Mapping of sub spectrum number ->
   * mobility
   */
  private final Map<Range<Integer>, Map<Integer, Double>> segmentMobilityRange;

  protected Range<Double> mobilityRange;
  protected MobilityType mobilityType;

  public IMSRawDataFileImpl(String dataFileName) throws IOException {
    super(dataFileName);

    frames = new TreeMap<>();
    frameNumbersCache = new Hashtable<>();
    dataMobilityRangeCache = new Hashtable<>();
    frameMsLevelCache = new Hashtable<>();
    segmentMobilityRange = new HashMap<>();

    mobilityRange = null;
    mobilityType = MobilityType.NONE;

    mdpStorage = new MobilityDataPointStorage();
  }

  @Override
  public synchronized Scan addScan(Scan newScan) throws IOException {

    // TODO: dirty hack - currently the frames are added to the scan and frame map
    if (this.mobilityType == MobilityType.NONE) {
      this.mobilityType = newScan.getMobilityType();
    }
    if (newScan.getMobilityType() != mobilityType) {
      throw new MSDKRuntimeException(
          "The mobility type specified in scan (" + newScan.getMobilityType()
              + ") does not match the mobility type of raw data file (" + getMobilityType() + ")");
    }

    if (newScan instanceof Frame) {
      Frame newFrame = (Frame) newScan;
      if (newScan instanceof StorableFrame) {
        scans.put(((StorableFrame) newScan).getFrameId(), (StorableFrame) newFrame);
        frames.put(((StorableFrame) newScan).getFrameId(), (StorableFrame) newFrame);
        return newFrame;
      }

      Range<Integer> segmentKey = getSegmentKeyForFrame(((Frame) newScan).getScanNumber());
      segmentMobilityRange.putIfAbsent(segmentKey, newFrame.getMobilities());

      final int storageId = storeDataPoints(newFrame.getDataPoints());
      StorableFrame storedFrame = new StorableFrame(newFrame, this,
          newFrame.getNumberOfDataPoints(), storageId);
      scans.put(storedFrame.getFrameId(), storedFrame);
      frames.put(storedFrame.getFrameId(), storedFrame);
      return storedFrame;
    } else {
      logger.info("Only frames should be added to an IMSRawDataFile");
      return super.addScan(newScan);
      /*if (mobilityRange == null) {
        mobilityRange = Range.singleton(newScan.getMobility());
      } else if (!mobilityRange.contains(newScan.getMobility())) {
        mobilityRange = mobilityRange.span(Range.singleton(newScan.getMobility()));
      }
      super.addScan(newScan);*/
    }
  }

  @Nonnull
  @Override
  public Collection<? extends Frame> getFrames() {
    return (Collection<? extends Frame>) frames.values();
  }

  @Nonnull
  @Override
  public Collection<? extends Frame> getFrames(int msLevel) {
    return frameMsLevelCache.computeIfAbsent(msLevel, level -> getFrames().stream()
        .filter(frame -> frame.getMSLevel() == msLevel).collect(Collectors.toSet()));
  }

  @Nullable
  @Override
  public Frame getFrame(int frameNum) {
    return (Frame) frames.get(frameNum);
  }

  @Override
  @Nonnull
  public List<Frame> getFrames(int msLevel, Range<Float> rtRange) {
    return frameMsLevelCache.get(msLevel).stream()
        .filter(frame -> rtRange.contains(frame.getRetentionTime())).collect(Collectors.toList());
  }

  @Nonnull
  @Override
  public Set<Integer> getFrameNumbers() {
    return frames.keySet();
  }

  @Nonnull
  @Override
  public Set<Integer> getFrameNumbers(int msLevel) {
    return frameNumbersCache.computeIfAbsent(msLevel, (key) -> {
      Set<Integer> frameNums = new HashSet<>();
      synchronized (frames) {
        for (Entry<Integer, StorableFrame> e : frames.entrySet()) {
          if (e.getValue().getMSLevel() == msLevel) {
            frameNums.add(e.getKey());
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

  @Nonnull
  @Override
  public Set<Integer> getFrameNumbers(int msLevel, @Nonnull Range<Float> rtRange) {
//     since {@link getFrameNumbers(int)} is prefiltered, this shouldn't lead to NPE
    return getFrameNumbers(msLevel).stream()
        .filter(frameNum -> rtRange.contains(getFrame(frameNum).getRetentionTime()))
        .collect(Collectors.toSet());
  }

  @Nonnull
  @Override
  public Range<Double> getDataMobilityRange() {
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

  @Nonnull
  @Override
  public MobilityType getMobilityType() {
    return mobilityType;
  }

  @Nonnull
  @Override
  public Range<Double> getDataMobilityRange(int msLevel) {
    if (dataMobilityRangeCache.get(msLevel) == null) {
      double lower = 1E10;
      double upper = -1E10;
      synchronized (frames) {
        for (Frame e : getFrames()) {
          if (e.getMSLevel() == msLevel &&
              e.getMobilityRange().lowerEndpoint() < lower) {
            lower = e.getMobilityRange().lowerEndpoint();
          }
          if (e.getMSLevel() == msLevel &&
              e.getMobilityRange().upperEndpoint() > upper) {
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

  @Override
  public double getMobilityForMobilitySpectrum(int frameNumber, int mobilitySpectrumNumber) {
    Map<Integer, Double> mobilityValues =
        segmentMobilityRange.entrySet().stream()
            .filter(entry -> entry.getKey().contains(frameNumber))
            .findFirst().get().getValue();
    return mobilityValues.getOrDefault(mobilitySpectrumNumber,
        MobilityMassSpectrum.DEFAULT_MOBILITY);
  }

  @Override
  public Map<Integer, Double> getMobilitiesForFrame(int frameNumber) {
    return segmentMobilityRange.entrySet().stream()
        .filter(entry -> entry.getKey().contains(frameNumber))
        .findFirst().get().getValue();
  }

  private Range<Integer> getSegmentKeyForFrame(int frameId) {
    return segmentMobilityRange.keySet().stream()
        .filter(segmentRange -> segmentRange.contains(frameId)).findFirst().get();
  }

  int storeDataPointsForMobilogram(List<MobilityDataPoint> dataPoints) throws IOException {
    return mdpStorage.storeDataPoints(dataPoints);
  }

  List<MobilityDataPoint> loadDatapointsForMobilogram(int storageId) throws IOException {
//    System.out.println("Reading data points for storage id " + storageId);
    return mdpStorage.readDataPoints(storageId);
  }

  public void removeDataPointsForMobilogram(int id) {
    try {
      mdpStorage.removeStoredDataPoints(id);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
