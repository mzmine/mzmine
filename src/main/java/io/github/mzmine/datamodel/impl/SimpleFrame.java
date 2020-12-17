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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.Mobilogram;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SimpleFrame extends SimpleScan implements Frame {

  private final int frameId;
  private final SortedMap<Integer, Scan> mobilityScans;
  private MobilityType mobilityType;
  /**
   * Mobility range of this frame. Updated when a scan is added.
   */
  private Range<Double> mobilityRange;

  public SimpleFrame(RawDataFile dataFile, int scanNumber, int msLevel,
      float retentionTime, double precursorMZ, int precursorCharge, int[] fragmentScans,
      DataPoint[] dataPoints,
      MassSpectrumType spectrumType,
      PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange, int frameId, MobilityType mobilityType,
      @Nonnull List<Integer> mobilityScanNumbers) {
    super(dataFile, scanNumber, msLevel, retentionTime, precursorMZ, precursorCharge, fragmentScans,
        dataPoints, spectrumType, polarity, scanDefinition, scanMZRange);

    this.frameId = frameId;
    this.mobilityType = mobilityType;
    mobilityRange = Range.singleton(0.d);

    mobilityScans = new TreeMap<>();
    for (int scannum : mobilityScanNumbers) {
      mobilityScans.put(scannum, null);
    }
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
  public Set<Integer> getMobilityScanNumbers() {
    return mobilityScans.keySet();
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

  public void addMobilityScans(List<Scan> mobilityScans) {
    for (Scan scan : mobilityScans) {
      addMobilityScan(scan);
    }
  }

  @Override
  public Scan getMobilityScan(int num) {
    throw new UnsupportedOperationException(
        "Mobility scans are not associated with SimpleFrames, only StorableFrames");
  }

  /**
   * @return Collection of mobility sub scans sorted by increasing scan num.
   */
  @Override
  @Nonnull
  public List<Scan> getMobilityScans() {
    throw new UnsupportedOperationException(
        "Mobility scans are not associated with SimpleFrames, only StorableFrames");
  }

  @Override
  public int getFrameId() {
    return frameId;
  }

  @Override
  public ImmutableList<Mobilogram> getMobilograms() {
    throw new UnsupportedOperationException("getMobilograms is not supported by SimpleFrame");
  }

  @Override
  public int addMobilogram(Mobilogram mobilogram) {
    throw new UnsupportedOperationException("addMobilogram is not supported by SimpleFrame");
  }

  @Override
  public void clearMobilograms() {
    throw new UnsupportedOperationException("clearMobilograms is not supported by SimpleFrame");
  }
}
