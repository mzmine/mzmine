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

package io.github.mzmine.project.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.id_ccscalibration.CCSCalibration;
import io.github.mzmine.util.MemoryMapStorage;
import it.unimi.dsi.fastutil.doubles.DoubleImmutableList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
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
  private final List<DoubleImmutableList> mobilitySegments = new ArrayList<>();

  protected Range<Double> mobilityRange;
  protected MobilityType mobilityType;
  protected CCSCalibration ccsCalibration = null;

  public IMSRawDataFileImpl(String dataFileName, @Nullable final String absolutePath,
      MemoryMapStorage storage) {
    super(dataFileName, absolutePath, storage);

    frameNumbersCache = new Hashtable<>();
    dataMobilityRangeCache = new Hashtable<>();
    frameMsLevelCache = new Hashtable<>();

    mobilityRange = null;
    mobilityType = MobilityType.NONE;
  }

  public IMSRawDataFileImpl(String dataFileName, @Nullable final String absolutePath,
      MemoryMapStorage storage, Color color) {
    super(dataFileName, absolutePath, storage, color);

    frameNumbersCache = new Hashtable<>();
    dataMobilityRangeCache = new Hashtable<>();
    frameMsLevelCache = new Hashtable<>();

    mobilityRange = null;
    mobilityType = MobilityType.NONE;
  }

  @Override
  public synchronized void addScan(Scan newScan) {

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

  @Override
  public void clearScans() {
    super.clearScans();
    frames.clear();
    frameNumbersCache.clear();
    dataMobilityRangeCache.clear();
    frameMsLevelCache.clear();
    mobilityRange = null;
    maxRawDataPoints = -1;
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

  @Override
  public int addMobilityValues(double[] mobilities) {
    for (int i = 0; i < mobilitySegments.size(); i++) {
      var mobilitySegment = mobilitySegments.get(i);
      if (mobilitySegment.size() != mobilities.length) {
        continue;
      }
      boolean equals = true;
      for (int j = 0; j < mobilitySegment.size(); j++) {
        if (Double.compare(mobilitySegment.getDouble(j), mobilities[j]) != 0) {
          equals = false;
          break;
        }
      }
      if (equals) {
        return i;
      }
    }
    mobilitySegments.add(new DoubleImmutableList(mobilities));
    if (mobilitySegments.size() > 10) {
      logger.finest(
          () -> "Registered " + mobilitySegments.size() + " mobility segments in file " + getName()
              + ".");
    }
    return mobilitySegments.size() - 1;
  }

  @Override
  public DoubleImmutableList getSegmentMobilities(int segment) {
    assert segment < mobilitySegments.size();
    return mobilitySegments.get(segment);
  }
}
