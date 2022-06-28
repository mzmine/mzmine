/*
 *  Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.MobilityScanStorage;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.util.DataPointUtils;
import it.unimi.dsi.fastutil.doubles.DoubleImmutableList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Loads a frame and it's subscans into ram.
 *
 * @author https://github.com/SteffenHeu
 */
public class CachedFrame implements Frame {

  private final double[] mzs;
  private final double[] intensities;
  private final Frame originalFrame;
  private List<MobilityScan> mobilityScans;
  private List<MobilityScan> sortedMobilityScans;

  public CachedFrame(Frame frame, double frameNoiseLevel, double mobilityScaNoiseLevel) {
    originalFrame = frame;

    double[] allmz = new double[frame.getNumberOfDataPoints()];
    double[] allintensities = new double[frame.getNumberOfDataPoints()];
    frame.getMzValues(allmz);
    frame.getIntensityValues(allintensities);

    double[][] data = DataPointUtils
        .getDatapointsAboveNoiseLevel(allmz, allintensities, frameNoiseLevel);

    mzs = data[0];
    intensities = data[1];

    this.mobilityScans = new ArrayList<>();
    for (MobilityScan scan : frame.getMobilityScans()) {
      mobilityScans.add(new CachedMobilityScan(scan, mobilityScaNoiseLevel));
    }
    sortedMobilityScans = mobilityScans.stream()
        .sorted(Comparator.comparingDouble(MobilityScan::getMobility)).collect(Collectors.toList());
  }


  public double[] getIntensities() {
    return intensities;
  }

  public double[] getMzs() {
    return mzs;
  }

  @Override
  public int getNumberOfMobilityScans() {
    return mobilityScans.size();
  }

  @NotNull
  @Override
  public MobilityType getMobilityType() {
    return originalFrame.getMobilityType();
  }

  @NotNull
  @Override
  public Range<Double> getMobilityRange() {
    return originalFrame.getMobilityRange();
  }

  @Nullable
  @Override
  public MobilityScan getMobilityScan(int num) {
    return mobilityScans.get(num);
  }

  @NotNull
  @Override
  public List<MobilityScan> getMobilityScans() {
    return mobilityScans;
  }

  @NotNull
  @Override
  public List<MobilityScan> getSortedMobilityScans() {
    return sortedMobilityScans;
  }

  @Override
  public MobilityScanStorage getMobilityScanStorage() {
    return originalFrame.getMobilityScanStorage();
  }

  @Override
  public double getMobilityForMobilityScanNumber(int mobilityScanIndex) {
    return originalFrame.getMobilityForMobilityScanNumber(mobilityScanIndex);
  }

  @Override
  public @Nullable DoubleImmutableList getMobilities() {
    return originalFrame.getMobilities();
  }

  @Override
  public @NotNull Set<PasefMsMsInfo> getImsMsMsInfos() {
    return originalFrame.getImsMsMsInfos();
  }

  @Nullable
  @Override
  public PasefMsMsInfo getImsMsMsInfoForMobilityScan(int mobilityScanNumber) {
    return originalFrame.getImsMsMsInfoForMobilityScan(mobilityScanNumber);
  }

  @Override
  public int getNumberOfDataPoints() {
    return mzs.length;
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return originalFrame.getSpectrumType();
  }

  @Override
  public double[] getMzValues(@NotNull double[] dst) {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Override
  public double getMzValue(int index) {
    return mzs[index];
  }

  @Override
  public double getIntensityValue(int index) {
    return intensities[index];
  }

  @Nullable
  @Override
  public Double getBasePeakMz() {
    return originalFrame.getBasePeakMz();
  }

  @Nullable
  @Override
  public Double getBasePeakIntensity() {
    return originalFrame.getBasePeakIntensity();
  }

  @Nullable
  @Override
  public Integer getBasePeakIndex() {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Nullable
  @Override
  public Range<Double> getDataPointMZRange() {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Nullable
  @Override
  public Double getTIC() {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Override
  public Stream<DataPoint> stream() {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @NotNull
  @Override
  public RawDataFile getDataFile() {
    return originalFrame.getDataFile();
  }

  @Override
  public int getScanNumber() {
    return originalFrame.getScanNumber();
  }

  @NotNull
  @Override
  public String getScanDefinition() {
    return originalFrame.getScanDefinition();
  }

  @Override
  public int getMSLevel() {
    return originalFrame.getMSLevel();
  }

  @Override
  public float getRetentionTime() {
    return originalFrame.getRetentionTime();
  }

  @NotNull
  @Override
  public Range<Double> getScanningMZRange() {
    return originalFrame.getScanningMZRange();
  }

  @Override
  public @Nullable MsMsInfo getMsMsInfo() {
    return null;
  }

  @NotNull
  @Override
  public PolarityType getPolarity() {
    return originalFrame.getPolarity();
  }

  @Nullable
  @Override
  public MassList getMassList() {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Override
  public void addMassList(@NotNull MassList massList) {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @NotNull
  @Override
  public Iterator<DataPoint> iterator() {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Override
  public int getMaxMobilityScanRawDataPoints() {
    return originalFrame.getMaxMobilityScanRawDataPoints();
  }

  @Override
  public int getTotalMobilityScanRawDataPoints() {
    return originalFrame.getTotalMobilityScanRawDataPoints();
  }

  @Override
  public int getMaxMobilityScanMassListDataPoints() {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Override
  public int getTotalMobilityScanMassListDataPoints() {
    throw new UnsupportedOperationException(
        "Not intended. This frame is used for visualisation only");
  }

  @Override
  public @Nullable Float getInjectionTime() {
    return originalFrame.getInjectionTime();
  }
}
