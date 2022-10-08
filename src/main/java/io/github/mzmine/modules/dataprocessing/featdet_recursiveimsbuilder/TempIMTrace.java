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

import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;

public class TempIMTrace {

  private static Logger logger = Logger.getLogger(TempMobilogram.class.getName());

  protected final TreeMap<Integer, BuildingIonMobilitySeries> mobilograms = new TreeMap<>();
  protected double lowestMz = Double.MAX_VALUE;
  protected double highestMz = Double.MIN_VALUE;
  protected double centerMz;

  public TempIMTrace() {

  }


  /**
   * Adds a data point if no data point of the same scan number is contained in this trace.
   *
   * @param mobilogram
   * @return
   */
  public BuildingIonMobilitySeries tryToAddMobilogram(BuildingIonMobilitySeries mobilogram) {
    var currentValue = mobilograms.putIfAbsent(mobilogram.getFrame().getFrameId(), mobilogram);
    if (currentValue == null) {
      updateValues();
    }
    return currentValue;
  }

  /**
   * @param mobilogram
   * @return The replaced data point
   */
  public BuildingIonMobilitySeries replaceMobilogram(BuildingIonMobilitySeries mobilogram) {
    final BuildingIonMobilitySeries replaced = mobilograms
        .put(mobilogram.getFrame().getFrameId(), mobilogram);
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
    for (BuildingIonMobilitySeries value : mobilograms.values()) {
      final double intensity = value.getSummedIntensity();
      final double mz = value.getAvgMZ();

      if (mz > highestMz) {
        highestMz = mz;
      }
      if (mz < lowestMz) {
        lowestMz = mz;
      }

      centerMz += mz * intensity;
      summedIntensities += value.getSummedIntensity();
    }
    centerMz /= summedIntensities;
  }

  public BuildingIonMobilitySeries keepBetterFittingDataPoint(BuildingIonMobilitySeries mob) {
    final BuildingIonMobilitySeries current = tryToAddMobilogram(mob);
    if (current == null) {
      return null;
    }

    final double currentDelta = Math.abs(centerMz - current.getAvgMZ());
    final double proposedDelta = Math.abs(centerMz - mob.getAvgMZ());
    if (currentDelta > proposedDelta) {
      var ceilingEntry = mobilograms.ceilingEntry(mob.getFrame().getFrameId() + 1);
      var floorEntry = mobilograms.floorEntry(mob.getFrame().getFrameId() - 1);

      if(ceilingEntry != null && floorEntry != null) {
        final double ceilingIntensity = ceilingEntry.getValue().getSummedIntensity();
        final double floorIntensity = floorEntry.getValue().getSummedIntensity();
        final double avgIntensity = (ceilingIntensity + floorIntensity) / 2;

        // only replace if the proposed intensity fits better
        if (Math.abs(avgIntensity - mob.getSummedIntensity()) < Math
            .abs(avgIntensity - current.getSummedIntensity())) {
          return replaceMobilogram(mob);
        }
      }
    }
    return mob;
  }

  public List<IonMobilitySeries> getMobilograms() {
    return new ArrayList<>(mobilograms.values());
  }

  public int getNumberOfDataPoints() {
    return mobilograms.values().stream().mapToInt(IonMobilitySeries::getNumberOfValues).sum();
  }

  public double getCenterMz() {
    return centerMz;
  }

  public void removeMobilograms(Collection<IonMobilitySeries> mobs) {
    mobs.forEach(m -> mobilograms.remove(m.getSpectrum(0).getFrame().getFrameId()));
  }
}
