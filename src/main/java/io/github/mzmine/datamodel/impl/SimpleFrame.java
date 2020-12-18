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
import io.github.mzmine.datamodel.MobilityMassSpectrum;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder.Mobilogram;
import java.util.ArrayList;
import io.github.mzmine.project.impl.StorableFrame;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SimpleFrame extends SimpleScan implements Frame {

  private MobilityType mobilityType;
  private final int numMobilitySpectra;
//  private final Map<Integer, Scan> mobilityScans;
  private final SortedMap<Integer, Scan> mobilityScans;
  private MobilityType mobilityType;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SimpleFrame)) {
      return false;
    }
    SimpleFrame that = (SimpleFrame) o;
    return getScanNumber() == that.getScanNumber() && getMSLevel() == that.getMSLevel()
        && Double.compare(that.getPrecursorMZ(), getPrecursorMZ()) == 0
        && getPrecursorCharge() == that.getPrecursorCharge()
        && Float.compare(that.getRetentionTime(), getRetentionTime()) == 0
        && getNumberOfDataPoints() == that.getNumberOfDataPoints()
        && Double.compare(that.getMobility(), getMobility()) == 0
        && Objects.equals(getDataPointMZRange(), that.getDataPointMZRange()) && Objects
        .equals(getHighestDataPoint(), that.getHighestDataPoint()) && Double.compare(getTIC(),
        that.getTIC()) == 0
        && getSpectrumType() == that.getSpectrumType() && getDataFile().equals(that.getDataFile())
        && Objects.equals(getMassLists(), that.getMassLists()) && getPolarity() == that
        .getPolarity() && Objects.equals(getScanDefinition(), that.getScanDefinition())
        && getScanningMZRange().equals(that.getScanningMZRange()) && getMobilityType() == that
        .getMobilityType() && getFrameId() == that.getFrameId();
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(getScanNumber(), getMSLevel(), getPrecursorMZ(), getPrecursorCharge(),
            getRetentionTime(),
            getDataPointMZRange(), getHighestDataPoint(), getTIC(), getSpectrumType(),
            getNumberOfDataPoints(),
            getDataFile(), getMassLists(), getPolarity(), getScanDefinition(), getScanningMZRange(),
            getMobility(), getMobilityType(), getFrameId());
  }
}
