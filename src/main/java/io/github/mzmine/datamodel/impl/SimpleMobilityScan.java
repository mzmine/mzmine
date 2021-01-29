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
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import java.nio.DoubleBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author https://github.com/SteffenHeu
 * @see io.github.mzmine.datamodel.MobilityScan
 */
public class SimpleMobilityScan implements MobilityScan {

  private static final Logger logger = Logger.getLogger(SimpleMobilityScan.class.getName());

  private final SimpleFrame frame;
  private final Set<MassList> massLists;
  private final int storageOffset;
  private final int numDataPoints;
  private int mobilityScamNumber;


  /*public SimpleMobilityScan(RawDataFile dataFile, int mobilityScamNumber, Frame frame,
      DataPoint dataPoints[]) {
    this.frame = frame;
    this.massLists = new HashSet<>();
    this.mobilityScamNumber = mobilityScamNumber;
  }*/

  public SimpleMobilityScan(int mobilityScamNumber, SimpleFrame frame,
      int storageOffset, int numDataPoints) {
    this.frame = frame;
    this.massLists = new HashSet<>();
    this.mobilityScamNumber = mobilityScamNumber;
    this.storageOffset = storageOffset;
    this.numDataPoints = numDataPoints;
  }

  @Override
  public int getNumberOfDataPoints() {
    return numDataPoints;
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return frame.getSpectrumType();
  }

  @Nonnull
  @Override
  public DoubleBuffer getMzValues() {
    return null;
  }

  @Nonnull
  @Override
  public DoubleBuffer getIntensityValues() {
    return null;
  }

  @Override
  public double[] getMzValues(@Nonnull double[] dst) {
    if (dst.length < getNumberOfDataPoints()) {
      dst = new double[getNumberOfDataPoints()];
    }
    frame.getMobilityScanMzValues(this, dst);
    return dst;
  }

  @Override
  public double[] getIntensityValues(@Nonnull double[] dst) {
    if (dst.length < getNumberOfDataPoints()) {
      dst = new double[getNumberOfDataPoints()];
    }
    frame.getMobilityScanIntensityValues(this, dst);
    return dst;
  }

  @Override
  public double getMzValue(int index) {
    return 0;
  }

  @Override
  public double getIntensityValue(int index) {
    return 0;
  }

  @Nullable
  @Override
  public Double getBasePeakMz() {
    return null;
  }

  @Nullable
  @Override
  public Double getBasePeakIntensity() {
    return null;
  }

  @Nullable
  @Override
  public Integer getBasePeakIndex() {
    return null;
  }

  @Nullable
  @Override
  public Range<Double> getDataPointMZRange() {
    return null;
  }

  @Nullable
  @Override
  public Double getTIC() {
    return null;
  }

  @Override
  public Stream<DataPoint> stream() {
    return null;
  }

  @Override
  public double getMobility() {
    return frame.getMobilityForMobilityScanNumber(mobilityScamNumber);
  }

  @Override
  public MobilityType getMobilityType() {
    return frame.getMobilityType();
  }

  @Override
  public Frame getFrame() {
    return frame;
  }

  @Override
  public float getRetentionTime() {
    return frame.getRetentionTime();
  }

  @Override
  public int getMobilityScamNumber() {
    return mobilityScamNumber;
  }

  @Nullable
  @Override
  public ImsMsMsInfo getMsMsInfo() {
    return frame.getImsMsMsInfoForMobilityScan(mobilityScamNumber);
  }

  @Override
  public synchronized void addMassList(final @Nonnull MassList massList) {
    // Remove all mass lists with same name, if there are any
    MassList currentMassLists[] = massLists.toArray(new MassList[0]);
    for (MassList ml : currentMassLists) {
      if (ml.getName().equals(massList.getName())) {
        removeMassList(ml);
      }
    }

    // Add the new mass list
    massLists.add(massList);
  }

  @Override
  public synchronized void removeMassList(final @Nonnull MassList massList) {
    // Remove the mass list
    massLists.remove(massList);
  }

  @Override
  @Nonnull
  public Set<MassList> getMassLists() {
    return Objects.requireNonNullElse(massLists, Collections.emptySet());
  }

  @Override
  public MassList getMassList(@Nonnull String name) {
    for (MassList ml : massLists) {
      if (ml.getName().equals(name)) {
        return ml;
      }
    }
    return null;
  }

  @Override
  public RawDataFile getDataFile() {
    return frame.getDataFile();
  }

  @Nonnull
  @Override
  public Iterator<DataPoint> iterator() {
    return null;
  }

  /**
   * @return Used to retrieve this scans storage offset when reading mz/intensity values. Not
   * intended for public usage, therefore not declared in {@link MobilityScan}.
   */
  public int getStorageOffset() {
    return storageOffset;
  }
}
