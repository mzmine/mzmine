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

import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.util.MemoryMapStorage;

/**
 * @author https://github.com/SteffenHeu
 * @see io.github.mzmine.datamodel.MobilityScan
 */
public class SimpleMobilityScan implements MobilityScan {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final RawDataFile dataFile;
  private final Frame frame;
  private DoubleBuffer mzValues, intensityValues;
  private int basePeak;
  private double totalIonCurrent;
  private int mobilityScamNumber;
  private Range<Double> mzRange;
  private Set<MassList> massLists;

  public SimpleMobilityScan(RawDataFile dataFile, int mobilityScamNumber, Frame frame,
      double mzValues[], double intensityValues[]) {
    this.dataFile = dataFile;
    this.frame = frame;

    this.mobilityScamNumber = mobilityScamNumber;
    setDataPoints(mzValues, intensityValues);
  }

  /**
   * @param dataPoints
   */
  public void setDataPoints(double mzValues[], double intensityValues[]) {

    assert mzValues.length == intensityValues.length;

    for (int i = 0; i < mzValues.length - 1; i++) {
      if (mzValues[i] > mzValues[i + 1]) {
        throw new IllegalArgumentException("The m/z values must be sorted in ascending order");
      }
    }

    MemoryMapStorage storage = dataFile.getMemoryMapStorage();
    try {
      this.mzValues = storage.storeData(mzValues);
      this.intensityValues = storage.storeData(intensityValues);
    } catch (IOException e) {
      e.printStackTrace();
      logger.log(Level.SEVERE,
          "Error while storing data points on disk, keeping them in memory instead", e);
      this.mzValues = DoubleBuffer.wrap(mzValues);
      this.intensityValues = DoubleBuffer.wrap(intensityValues);
    }

    totalIonCurrent = 0;

    // find m/z range and base peak
    if (intensityValues.length > 0) {

      basePeak = 0;
      mzRange = Range.closed(mzValues[0], mzValues[mzValues.length - 1]);

      for (int i = 0; i < intensityValues.length; i++) {

        if (intensityValues[i] > intensityValues[basePeak]) {
          basePeak = i;
        }

        totalIonCurrent += intensityValues[i];

      }

    } else {
      mzRange = Range.singleton(0.0);
      basePeak = -1;
    }
  }



  @Nonnull
  @Override
  public Range<Double> getDataPointMZRange() {
    return mzRange;
  }

  @Nullable
  @Override
  public int getBasePeak() {
    return basePeak;
  }

  @Override
  public double getTIC() {
    return totalIonCurrent;
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return frame.getSpectrumType();
  }

  @Override
  public int getNumberOfDataPoints() {
    return mzValues.capacity();
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
  public DoubleBuffer getMzValues() {
    return mzValues;
  }

  @Override
  public DoubleBuffer getIntensityValues() {
    return intensityValues;
  }
}
