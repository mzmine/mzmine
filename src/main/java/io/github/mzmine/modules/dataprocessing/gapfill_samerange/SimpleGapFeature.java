/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
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
