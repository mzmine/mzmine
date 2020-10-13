package io.github.mzmine.project.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import java.io.IOException;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nonnull;

public class StorableFrame extends StorableScan implements Frame {

  private final int frameId;
  /**
   * key = scan Num, value = storage id  // TODO do we need this?
   */
  private final SortedMap<Integer, Integer> mobilityScanIds;
  /**
   * key = scan num, value = mobility scan // TODO do we need this?
   */
  private final SortedMap<Integer, Scan> mobilityScans;
  /**
   * Mobility range of this frame. Updated when a scan is added.
   */
  private Range<Double> mobilityRange;

  private final int[] mobilityScanNumbers;

  /**
   * @param originalFrame
   * @param rawDataFile
   * @param numberOfDataPoints
   * @param storageID
   */
  public StorableFrame(Frame originalFrame,
      RawDataFileImpl rawDataFile, int numberOfDataPoints, int storageID) throws IOException {
    super(originalFrame, rawDataFile, numberOfDataPoints, storageID);

    frameId = originalFrame.getFrameId();
    mobilityScanIds = new TreeMap<>();
    mobilityScans = new TreeMap<>();

    for (Scan mobilityScan : originalFrame.getMobilityScans()) {
      final int storageId = rawDataFile.storeDataPoints(mobilityScan.getDataPoints());
      mobilityScanIds.put(mobilityScan.getScanNumber(), storageId);
      mobilityScans.put(mobilityScan.getScanNumber(),
          new StorableScan(mobilityScan, rawDataFile, mobilityScan.getNumberOfDataPoints(),
              storageID));

      if (mobilityScan.getMobility() < mobilityRange.lowerEndpoint()) {
        mobilityRange = Range.closed(mobilityScan.getMobility(), mobilityRange.upperEndpoint());
      }
      if (mobilityScan.getMobility() > mobilityRange.upperEndpoint()) {
        mobilityRange = Range.closed(mobilityRange.lowerEndpoint(), mobilityScan.getMobility());
      }
    }

    mobilityScanNumbers = originalFrame.getMobilityScanNumbers();
  }

  public StorableFrame(RawDataFileImpl rawDataFile, int storageID, int numberOfDataPoints,
      int scanNumber, int msLevel, double retentionTime, double mobility, double precursorMZ,
      int precursorCharge, int[] fragmentScans,
      MassSpectrumType spectrumType,
      PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange, int frameId, MobilityType mobilityType,
      Range<Double> mobilityRange, int[] mobilityScanNumbers) {
    super(rawDataFile, storageID, numberOfDataPoints, scanNumber, msLevel, retentionTime, mobility,
        precursorMZ, precursorCharge, fragmentScans, spectrumType, polarity, scanDefinition,
        scanMZRange);
    this.frameId = frameId;
    this.mobilityScanNumbers = mobilityScanNumbers;
    this.mobilityRange = mobilityRange;

    // TODO do we need these?
    mobilityScanIds = new TreeMap<>();
    mobilityScans = new TreeMap<>();

  }

  @Override
  public int getFrameId() {
    return frameId;
  }

  @Override
  public int getNumberOfMobilityScans() {
    return mobilityScanIds.size();
  }

  @Override
  public int[] getMobilityScanNumbers() {
    return mobilityScanIds.keySet().stream().mapToInt(Integer::intValue).toArray();
  }

  @Nonnull
  @Override
  public Range<Double> getMobilityRange() {
    return mobilityRange;
  }

  @Nonnull
  @Override
  public Scan getMobilityScan(int scanNum) {
    return mobilityScans.get(scanNum);
  }

  @Nonnull
  @Override
  public Collection<Scan> getMobilityScans() {
    return mobilityScans.values();
  }
}
