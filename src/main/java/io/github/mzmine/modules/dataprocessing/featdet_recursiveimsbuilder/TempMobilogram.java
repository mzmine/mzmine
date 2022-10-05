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

package io.github.mzmine.modules.dataprocessing.featdet_recursiveimsbuilder;

import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.RetentionTimeMobilityDataPoint;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

public class TempMobilogram {

  private static Logger logger = Logger.getLogger(TempMobilogram.class.getName());

  protected final TreeMap<Integer, RetentionTimeMobilityDataPoint> datapoints = new TreeMap<>();
  protected double lowestMz = Double.MAX_VALUE;
  protected double highestMz = Double.MIN_VALUE;
  protected double centerMz;

  public TempMobilogram() {

  }


  /**
   * Adds a data point if no data point of the same scan number is contained in this trace.
   *
   * @param dp
   * @return
   */
  public RetentionTimeMobilityDataPoint tryToAddDataPoint(RetentionTimeMobilityDataPoint dp) {
    var currentValue = datapoints.putIfAbsent(dp.getMobilityScan().getMobilityScanNumber(), dp);
    if (currentValue == null) {
      updateValues();
    }
    return currentValue;
  }

  /**
   * @param dp
   * @return The replaced data point
   */
  public RetentionTimeMobilityDataPoint replaceDataPoint(RetentionTimeMobilityDataPoint dp) {

    final RetentionTimeMobilityDataPoint replaced = datapoints
        .put(dp.getMobilityScan().getMobilityScanNumber(), dp);
    if (replaced == null) {
      logger.fine(() -> "Data point did not replace another data point");
    }
    updateValues();
    return replaced;
  }

  public double getLowestMz() {
    return lowestMz;
  }

  public void setLowestMz(double lowestMz) {
    this.lowestMz = lowestMz;
  }

  public double getHighestMz() {
    return highestMz;
  }

  public void setHighestMz(double highestMz) {
    this.highestMz = highestMz;
  }

  private void updateValues() {
    centerMz = 0d;
    double summedIntensities = 0d;
    for (RetentionTimeMobilityDataPoint value : datapoints.values()) {
      final double intensity = value.getIntensity();
      final double mz = value.getMZ();

      if (mz > highestMz) {
        highestMz = mz;
      }
      if (mz < lowestMz) {
        lowestMz = mz;
      }

      centerMz += mz * intensity;
      summedIntensities += intensity;
    }
    centerMz /= summedIntensities;
  }

  public RetentionTimeMobilityDataPoint keepBetterFittingDataPoint(
      RetentionTimeMobilityDataPoint dp) {
    final RetentionTimeMobilityDataPoint current = tryToAddDataPoint(dp);
    if (current == null) {
      return null;
    }

    final double currentDelta = Math.abs(centerMz - current.getMZ());
    final double proposedDelta = Math.abs(centerMz - dp.getMZ());
    if (currentDelta < proposedDelta) {
      return dp;
    }
    var ceilingEntry = datapoints.ceilingEntry(dp.getMobilityScan().getMobilityScanNumber() + 1);
    var floorEntry = datapoints.floorEntry(dp.getMobilityScan().getMobilityScanNumber() - 1);
    if (ceilingEntry != null && floorEntry != null) {
      final double ceilingIntensity = ceilingEntry.getValue().getIntensity();
      final double floorIntensity = floorEntry.getValue().getIntensity();
      final double avg = (ceilingIntensity + floorIntensity) / 2;
      if (Math.abs(avg - dp.getIntensity()) < Math.abs(avg - current.getIntensity())) {
        return replaceDataPoint(dp);
      }
    }

    return dp;
  }

  public BuildingIonMobilitySeries toBuildingSeries(@Nullable MemoryMapStorage storage) {
    final int numValues = datapoints.size();
    double[] mzs = new double[numValues];
    double[] intensities = new double[numValues];
    List<MobilityScan> scans = new ArrayList<>();

    int i = 0;
    for (RetentionTimeMobilityDataPoint value : datapoints.values()) {
      mzs[i] = value.getMZ();
      intensities[i] = value.getIntensity();
      scans.add(value.getMobilityScan());
      i++;
    }
    return new BuildingIonMobilitySeries(storage, mzs, intensities, scans);
  }
}
