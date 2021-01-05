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

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IonMobilityTimeSeries;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * Used to store ion mobility-LC-MS data
 *
 * @author https://github.com/SteffenHeu
 */
public class SimpleIonMobilityTimeSeries implements IonMobilityTimeSeries {

  private static final Logger logger = Logger.getLogger(SimpleMsTimeSeries.class.getName());

  protected final List<SimpleIonMobilitySeries> simpleIonMobilitySeries;
  protected final List<Frame> frames;
  protected DoubleBuffer intensityValues;
  protected DoubleBuffer mzValues;

  public SimpleIonMobilityTimeSeries(@Nonnull MemoryMapStorage storage, @Nonnull List<SimpleIonMobilitySeries> simpleIonMobilitySeries) {

    frames = new ArrayList<Frame>(simpleIonMobilitySeries.size());
    this.simpleIonMobilitySeries = simpleIonMobilitySeries;

    double[] summedIntensities = new double[simpleIonMobilitySeries.size()];
    double[] weightedMzs = new double[simpleIonMobilitySeries.size()];

    for(int i = 0; i < simpleIonMobilitySeries.size(); i ++) {
      SimpleIonMobilitySeries ims = simpleIonMobilitySeries.get(i);
      frames.add(ims.getScans().get(0).getFrame());

      double[] intensities = ims.getIntensityValues().array();
      double[] mzValues = ims.getMzValues().array();
      summedIntensities[i] = Arrays.stream(intensities).sum();

      // calculate an intensity weighted average for mz
      double weightedMz = 0;
      for(int j = 0; j < mzValues.length; j++) {
        weightedMz += mzValues[j] * (intensities[j] / summedIntensities[i]);
      }
      weightedMzs[i] = weightedMz;
    }

    try {
      intensityValues = storage.storeData(weightedMzs);
      mzValues = storage.storeData(weightedMzs);
    } catch (IOException e) {
      e.printStackTrace();
      intensityValues = DoubleBuffer.wrap(summedIntensities);
      mzValues = DoubleBuffer.wrap(weightedMzs);
    }

  }

  @Override
  public DoubleBuffer getIntensityValues() {
    return intensityValues;
  }

  @Override
  public DoubleBuffer getMzValues() {
    return mzValues;
  }

  /**
   *
   * @return The frames.
   */
  @Override
  public List<Frame> getScans() {
    return frames;
  }

  @Override
  public Number getX(int index) {
    return getRetentionTime(index);
  }

}