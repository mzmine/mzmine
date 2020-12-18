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
import io.github.mzmine.datamodel.MobilityMassSpectrum;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SimpleFrame extends SimpleScan implements Frame {

  private MobilityType mobilityType;
  private final int numMobilitySpectra;
//  private final Map<Integer, Scan> mobilityScans;
  /**
   * Mobility range of this frame. Updated when a scan is added.
   */
  private Range<Double> mobilityRange;
  private Map<Integer, Double> mobilities;

  public SimpleFrame(RawDataFile dataFile, int scanNumber, int msLevel,
      float retentionTime, double precursorMZ, int precursorCharge, /*int[] fragmentScans,*/
      DataPoint[] dataPoints,
      MassSpectrumType spectrumType,
      PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange, MobilityType mobilityType,
      final int numMobilitySpectra, Map<Integer, Double> mobilities) {
    super(dataFile, scanNumber, msLevel, retentionTime, precursorMZ,
        precursorCharge, /*fragmentScans,*/
        dataPoints, spectrumType, polarity, scanDefinition, scanMZRange);

    this.mobilityType = mobilityType;
    mobilityRange = Range.singleton(0.d);
    this.numMobilitySpectra = numMobilitySpectra;
    this.mobilities = mobilities;
  }

  /**
   * @return The number of mobility resolved sub scans.
   */
  @Override
  public int getNumberOfMobilityScans() {
    return numMobilitySpectra;
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
    return mobilities.keySet();
  }

  @Override
  @Nonnull
  public Range<Double> getMobilityRange() {
    throw new UnsupportedOperationException(
        "Mobility scans are not associated with SimpleFrames, only StorableFrames");
  }

  @Override
  public MobilityMassSpectrum getMobilityScan(int num) {
    throw new UnsupportedOperationException(
        "Mobility scans are not associated with SimpleFrames, only StorableFrames");
  }

  /**
   * @return Collection of mobility sub scans sorted by increasing scan num.
   */
  @Override
  @Nonnull
  public List<MobilityMassSpectrum> getMobilityScans() {
    throw new UnsupportedOperationException(
        "Mobility scans are not associated with SimpleFrames, only StorableFrames");
  }

  @Override
  public double getMobilityForSubSpectrum(int subSpectrumIndex) {
    return mobilities.getOrDefault(subSpectrumIndex, MobilityMassSpectrum.DEFAULT_MOBILITY);
  }

  @Override
  public Map<Integer, Double> getMobilities() {
    return mobilities;
  }
}
