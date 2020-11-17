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
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

  private final List<Integer> mobilityScanNumbers;

  /**
   * Creates a storable frame and also stores the mobility resolved scans.
   *
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
    mobilityRange = Range.singleton(0.0);

    for (Scan mobilityScan : originalFrame.getMobilityScans()) {

      StorableScan storedScan = null;

      if (!(mobilityScan instanceof StorableScan)) {
        final int storageId = rawDataFile.storeDataPoints(mobilityScan.getDataPoints());

        storedScan = new StorableScan(mobilityScan, rawDataFile,
            mobilityScan.getNumberOfDataPoints(), storageId);
        mobilityScanIds.put(storedScan.getScanNumber(), storageId);
        mobilityScans.put(storedScan.getScanNumber(), storedScan);
      } else {
        storedScan = (StorableScan) mobilityScan;
      }
      rawDataFile.addScan(storedScan);

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
      int scanNumber, int msLevel, double retentionTime, double precursorMZ,
      int precursorCharge, int[] fragmentScans,
      MassSpectrumType spectrumType,
      PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange, int frameId, @Nonnull MobilityType mobilityType,
      @Nonnull Range<Double> mobilityRange, @Nonnull List<Integer> mobilityScanNumbers) {

    super(rawDataFile, storageID, numberOfDataPoints, scanNumber, msLevel, retentionTime,
        precursorMZ, precursorCharge, fragmentScans, spectrumType, polarity, scanDefinition,
        scanMZRange);

    this.frameId = frameId;
    this.mobilityScanNumbers = mobilityScanNumbers;
    this.mobilityRange = mobilityRange;
    this.mobilityType = mobilityType;

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
  public List<Integer> getMobilityScanNumbers() {
    return new ArrayList<>(mobilityScanIds.keySet());
  }

  @Nonnull
  @Override
  public Range<Double> getMobilityRange() {
    return mobilityRange;
  }

  @Nonnull
  @Override
  public Scan getMobilityScan(int scanNum) {
    return Objects.requireNonNull(
        mobilityScans.computeIfAbsent(scanNum, i -> rawDataFile.getScan(scanNum)));
  }

  @Nonnull
  @Override
  public List<Scan> getMobilityScans() {
    return new ArrayList<>(mobilityScans.values());
  }
}
