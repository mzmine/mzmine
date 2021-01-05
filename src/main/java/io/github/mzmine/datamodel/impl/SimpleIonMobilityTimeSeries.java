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
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * Used to store ion mobility-LC-MS data
 */
public class SimpleIonMobilityTimeSeries implements IonMobilityTimeSeries {

  private static final Logger logger = Logger.getLogger(SimpleMsTimeSeries.class.getName());

  protected final List<IonMobilitySeries> ionMobilitySeries;
  protected final List<Frame> frames;
  protected DoubleBuffer intensityValues;
  protected DoubleBuffer mzValues;

  public SimpleIonMobilityTimeSeries(@Nonnull MemoryMapStorage storage, @Nonnull List<Frame> frames) {

    // todo calculate summed intensities/mzs based on the IonMobilitySeries

    ionMobilitySeries = new ArrayList<>();
    this.frames = frames;

  }

  @Override
  public DoubleBuffer getIntensityValues() {
    return null;
  }

  @Override
  public DoubleBuffer getMzValues() {
    return null;
  }

  /**
   *
   * @return The frames.
   */
  @Override
  public List<Frame> getScans() {
    return null;
  }

  @Override
  public float getRetentionTime(int index) {
    return getScans().get(index).getRetentionTime();
  }
}