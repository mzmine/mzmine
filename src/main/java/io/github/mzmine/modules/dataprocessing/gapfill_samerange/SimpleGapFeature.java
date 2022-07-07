/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.gapfill_samerange;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.util.MemoryMapStorage;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class SimpleGapFeature {

  private final List<Scan> scans = new ArrayList<>();
  private final DoubleList mzs = new DoubleArrayList();
  private final DoubleList intensities = new DoubleArrayList();

  public void addDataPoint(Scan scan, double mz, double intensity) {
    scans.add(scan);
    mzs.add(mz);
    intensities.add(intensity);
  }

  /**
   * Create an ion time series from the current data
   *
   * @param storage store data on disk in memory mapped file
   * @return an ion time series
   */
  public IonTimeSeries<Scan> toIonTimeSeries(@Nullable MemoryMapStorage storage) {
    return new SimpleIonTimeSeries(storage, mzs.toDoubleArray(), intensities.toDoubleArray(),
        scans);
  }

  public List<Scan> getScans() {
    return scans;
  }

  public DoubleList getMzs() {
    return mzs;
  }

  public DoubleList getIntensities() {
    return intensities;
  }

  public boolean isEmpty() {
    return scans.isEmpty();
  }

  public boolean hasData() {
    return !isEmpty();
  }

  public int size() {
    return scans.size();
  }

  public void addDataPoint(Scan scan, DataPoint dataPoint) {
    addDataPoint(scan, dataPoint.getMZ(), dataPoint.getIntensity());
  }

  private boolean isZeroIntensity(int i) {
    return Double.compare(intensities.getDouble(i), 0) <= 0;
  }

  public void removeEdgeZeroIntensities() {
    // remove leading zeros
    for (int i = 0; i < scans.size(); i++) {
      if (isZeroIntensity(i)) {
        remove(i);
      } else {
        break;
      }
    }
    // remove trailing zeros
    for (int i = scans.size() - 1; i >= 0; i--) {
      if (isZeroIntensity(i)) {
        remove(i);
      } else {
        break;
      }
    }
  }

  public void removeZeroIntensities() {
    for (int i = 0; i < scans.size(); i++) {
      if (isZeroIntensity(i)) {
        remove(i);
      }
    }
  }

  /**
   * Remove all zeros but keep one zero at edges
   *
   * @param keepEdgeZeros keep one zero at edges if available
   */
  public void removeZeroIntensities(boolean keepEdgeZeros) {
    if (!keepEdgeZeros) {
      removeZeroIntensities();
      return;
    }
    boolean wasZero = false;
    boolean peakFound = false;
    for (int i = 0; i < scans.size(); i++) {
      if (isZeroIntensity(i)) {
        // keep one zero after peak
        if (peakFound) {
          peakFound = false;
          continue;
        }

        if (wasZero) {
          remove(i - 1);
        }
        wasZero = true;
      } else {
        peakFound = true;
        wasZero = false;
      }
    }
  }

  private void remove(int index) {
    scans.remove(index);
    mzs.removeDouble(index);
    intensities.removeDouble(index);
  }
}
