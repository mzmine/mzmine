/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetector;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.TDFUtils;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UndloadedTDFFrame extends SimpleScan implements Frame {

  private final int numMobilityScans;
  private final Set<PasefMsMsInfo> precursorInfos;
  private Double mobilityScanNoiseLevel = null;
  private final int totalMobilityScanPeaks;

  public UndloadedTDFFrame(@NotNull RawDataFile dataFile, int scanNumber, int msLevel, float retentionTime,
      double[] mzValues, double[] intensityValues, MassSpectrumType spectrumType,
      PolarityType polarity, String scanDefinition, Range<Double> scanMZRange,
      int numMobilityScans, int totalNumPeaks) {
    super(dataFile, scanNumber, msLevel, retentionTime, null, mzValues, intensityValues,
        spectrumType, polarity, scanDefinition, scanMZRange);
    this.numMobilityScans = numMobilityScans;
    this.precursorInfos = new HashSet<>(0);
    this.totalMobilityScanPeaks = totalNumPeaks;
  }

  public double getMobilityScanNoiseLevel() {
    if (mobilityScanNoiseLevel == null) {
      throw new IllegalStateException(
          "Mobility scan noise level in data file " + getDataFile().getName()
              + " has not been set.");
    }
    return mobilityScanNoiseLevel;
  }

  public void setMobilityScanNoiseLevel(Double mobilityScanNoiseLevel) {
    this.mobilityScanNoiseLevel = mobilityScanNoiseLevel;
  }

  @Override
  public int getNumberOfMobilityScans() {
    return numMobilityScans;
  }

  @Override
  public @NotNull MobilityType getMobilityType() {
    return MobilityType.TIMS;
  }

  @Override
  public @NotNull Range<Double> getMobilityRange() {
    final double[] mobilities = ((TdfImsRawDataFileImpl) getDataFile()).getMobilitiesForFrame(
        getFrameId());
    double ook0start = Math.min(mobilities[0], mobilities[mobilities.length - 1]);
    double ook0end = Math.max(mobilities[0], mobilities[mobilities.length - 1]);
    return Range.closed(ook0start, ook0end);
  }

  @Override
  public @Nullable MobilityScan getMobilityScan(int num) {
    return getMobilityScan(num, ((TdfImsRawDataFileImpl) getDataFile()).getTdfUtils());
  }

  public @Nullable MobilityScan getMobilityScan(int num, @NotNull TDFUtils utils) {
    assert num < getNumberOfMobilityScans() && num >= 0;
    return new UnloadedTdfMobilityScan(this, num, ((TdfImsRawDataFileImpl) getDataFile()).getTdfUtils());
  }

  @Override
  public @NotNull List<MobilityScan> getMobilityScans() {
    return getMobilityScans(((TdfImsRawDataFileImpl) getDataFile()).getTdfUtils());
  }

  public @NotNull List<MobilityScan> getMobilityScans(@NotNull final TDFUtils utils) {
    List<io.github.mzmine.datamodel.MobilityScan> scans = new ArrayList<>(numMobilityScans);

    for (int i = 0; i < numMobilityScans; i++) {
      scans.add(new UnloadedTdfMobilityScan(this, i, utils));
    }
    return scans;
  }

  public @NotNull List<MobilityScan> getMobilityScansPreloaded(@NotNull final TDFUtils utils) {
    List<MobilityScan> scans = new ArrayList<>(numMobilityScans);
    final List<double[][]> mzIntensities = utils.loadDataPointsForFrame(getFrameId(), 0,
        getNumberOfMobilityScans());

    for (int i = 0; i < mzIntensities.size(); i++) {
      scans.add(new UnloadedTdfMobilityScan(this, i, mzIntensities.get(i)[0], mzIntensities.get(i)[1]));
    }
    return scans;
  }

  public @NotNull List<MassList> getMobilityScanMassListsPreloaded(@NotNull final TDFUtils utils) {
    final TDFUtils tdfUtils =
        utils != null ? utils : ((TdfImsRawDataFileImpl) getDataFile()).getTdfUtils();

    final List<double[][]> doubles = tdfUtils.loadDataPointsForFrame(getFrameId(),
       0, getNumberOfMobilityScans());

    final List<MassList> scans = new ArrayList<>(numMobilityScans);
    for (int i = 0; i < doubles.size(); i++) {
      double[][] mzIntensities = MZmineCore.getModuleInstance(CentroidMassDetector.class)
          .getMassValues(doubles.get(i)[0], doubles.get(i)[1], getMobilityScanNoiseLevel());
      scans.add(new UnloadedTdfMobilityScan(this, i, mzIntensities[0], mzIntensities[1]));
    }
    return scans;
  }

  @Override
  public @NotNull List<MobilityScan> getSortedMobilityScans() {
    return getMobilityScans().stream().sorted(Comparator.comparingDouble(MobilityScan::getMobility))
        .toList();
  }

  @Override
  public double getMobilityForMobilityScanNumber(int mobilityScanIndex) {
    return ((TdfImsRawDataFileImpl) getDataFile()).getMobilitiesForFrame(
        getFrameId())[mobilityScanIndex];
  }

  @Override
  public @Nullable DoubleBuffer getMobilities() {
    return DoubleBuffer.wrap(
        ((TdfImsRawDataFileImpl) getDataFile()).getMobilitiesForFrame(getFrameId()));
  }

  @Override
  public @NotNull Set<PasefMsMsInfo> getImsMsMsInfos() {
    return precursorInfos;
  }

  @Nullable
  @Override
  public PasefMsMsInfo getImsMsMsInfoForMobilityScan(int mobilityScanNumber) {
    if (precursorInfos == null) {
      return null;
    }
    Optional<PasefMsMsInfo> pcInfo = precursorInfos.stream()
        .filter(info -> info.getSpectrumNumberRange().contains(mobilityScanNumber)).findFirst();
    return pcInfo.orElse(null);
  }

  @Override
  public int getMaxMobilityScanRawDataPoints() {
    return getNumberOfDataPoints(); // no mobility scan should have more dp than the merged frame
  }

  @Override
  public int getTotalMobilityScanRawDataPoints() {
    return totalMobilityScanPeaks;
  }

  @Override
  public int getMaxMobilityScanMassListDataPoints() {
    return getMaxMobilityScanRawDataPoints();
  }

  @Override
  public int getTotalMobilityScanMassListDataPoints() {
    return getTotalMobilityScanRawDataPoints();
  }
}
