package io.github.mzmine.modules.dataprocessing.featdet_imsbuilder;

import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

public class TempIMTrace {

  private static Logger logger = Logger.getLogger(TempMobilogram.class.getName());

  protected final SortedMap<Integer, BuildingIonMobilitySeries> mobilograms = new TreeMap<>();
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
    var currentValue = mobilograms.putIfAbsent(mobilogram.getFrameNumber(), mobilogram);
    if (currentValue == null) {
      updateValues();
    }
    return currentValue;
  }

  /**
   * @param mobilogram
   * @return The replaced data point
   */
  public BuildingIonMobilitySeries replaceDataPoint(BuildingIonMobilitySeries mobilogram) {
    final BuildingIonMobilitySeries replaced = mobilograms
        .put(mobilogram.getFrameNumber(), mobilogram);
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

  public BuildingIonMobilitySeries keepBetterFittingDataPoint(
      BuildingIonMobilitySeries dp) {
    final BuildingIonMobilitySeries current = tryToAddMobilogram(dp);
    if (current == null) {
      return null;
    }

    final double currentDelta = Math.abs(centerMz - current.getAvgMZ());
    final double proposedDelta = Math.abs(centerMz - dp.getAvgMZ());
    if (currentDelta > proposedDelta) {
      return replaceDataPoint(dp);
    }
    return dp;
  }

  public List<IonMobilitySeries> getMobilograms() {
    return new ArrayList<>(mobilograms.values());
  }
}
