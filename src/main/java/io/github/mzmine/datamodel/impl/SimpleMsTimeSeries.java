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

import io.github.mzmine.datamodel.MsTimeSeries;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * Used to store LC-MS data.
 */
public class SimpleMsTimeSeries implements MsTimeSeries<Scan> {

  private static final Logger logger = Logger.getLogger(SimpleMsTimeSeries.class.getName());

  protected final List<Scan> scans;
  protected DoubleBuffer intensityValues;
  protected DoubleBuffer mzValues;

  public SimpleMsTimeSeries(@Nonnull MemoryMapStorage storage, @Nonnull double[] mzValues,
      @Nonnull double[] intensityValues, @Nonnull List<Scan> scans) {
    if (mzValues.length != intensityValues.length || mzValues.length != scans.size()) {
      throw new IllegalArgumentException("Length of mz, intensity and/or scans does not match.");
    }

    this.scans = scans;
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
  }

  @Override
  public DoubleBuffer getIntensityValues() {
    return intensityValues;
  }

  @Override
  public DoubleBuffer getMzValues() {
    return mzValues;
  }

  @Override
  public List<Scan> getScans() {
    return scans;
  }

  @Override
  public float getRetentionTime(int index) {
    return 0;
  }
}