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

package io.github.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SimpleFrame extends SimpleScan implements Frame {

  private final int frameId;
  private MobilityType mobilityType;
  private final SortedMap<Integer, Scan> mobilityScans;
  /**
   * Mobility range of this frame. Updated when a scan is added.
   */
  private Range<Double> mobilityRange;

  public SimpleFrame(RawDataFile dataFile, int scanNumber, int msLevel,
      double retentionTime, double precursorMZ, int precursorCharge, int[] fragmentScans,
      DataPoint[] dataPoints,
      MassSpectrumType spectrumType,
      PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange, int frameId, MobilityType mobilityType) {
    super(dataFile, scanNumber, msLevel, retentionTime, precursorMZ, precursorCharge, fragmentScans,
        dataPoints, spectrumType, polarity, scanDefinition, scanMZRange);

    this.frameId = frameId;
    this.mobilityType = mobilityType;
    mobilityScans = new TreeMap<>();
    mobilityRange = Range.singleton(0.d);
  }

  /**
   * @return The number of mobility resolved sub scans.
   */
  @Override
  public int getNumberOfMobilityScans() {
    return mobilityScans.size();
  }

  @Override
  @Nonnull
  public MobilityType getMobilityType() {
    return mobilityType;
  }

  /**
   * @return Scan numbers of sub scans.
   */
  @Override
  public List<Integer> getMobilityScanNumbers() {
    return new ArrayList<>(mobilityScans.keySet());
  }

  @Override
  @Nonnull
  public Range<Double> getMobilityRange() {
    return mobilityRange;
  }

  /**
   * Adds the scan as a sub scan. The scan number is taken from the mobility scan.
   *
   * @param mobilityScan The scan to add.
   * @return {@link Map#put(Object, Object)}
   */
  @Nullable
  public Scan addMobilityScan(@Nonnull Scan mobilityScan) {
    if (mobilityScan.getMobility() < mobilityRange.lowerEndpoint()) {
      mobilityRange = Range.closed(mobilityScan.getMobility(), mobilityRange.upperEndpoint());
    }
    if (mobilityScan.getMobility() > mobilityRange.upperEndpoint()) {
      mobilityRange = Range.closed(mobilityRange.lowerEndpoint(), mobilityScan.getMobility());
    }
    return mobilityScans.put(mobilityScan.getScanNumber(), mobilityScan);
  }

  @Override
  public Scan getMobilityScan(int scanNum) {
    return mobilityScans.get(scanNum);
  }

  /**
   * @return Collection of mobility sub scans sorted by increasing scan num.
   */
  @Override
  @Nonnull
  public List<Scan> getMobilityScans() {
    return new ArrayList<>(mobilityScans.values());
  }

  @Override
  public int getFrameId() {
    return frameId;
  }
}
