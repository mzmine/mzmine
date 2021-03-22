package io.github.mzmine.modules.dataprocessing.featdet_imsbuilder;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import java.util.ArrayList;
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

  public boolean isConsecutive(int reqConsecutive, List<Frame> eligibleFrames) {

    int numConsecutive = 0;

    int index = 0;
    int prevIndex = 0;
    for (IonMobilitySeries mobilogram : mobilograms.values()) {
      final Frame frame = mobilogram.getSpectrum(0).getFrame();
      while (frame != eligibleFrames.get(index)) {
        index++;
      }

      if (index - prevIndex <= 1) {
        numConsecutive++;
        if (numConsecutive >= reqConsecutive) {
          return true;
        }
      } else {
        numConsecutive = 0;
      }

      prevIndex = index;
    }

    return false;
  }

  public int getNumberOfDataPoints() {
    return mobilograms.values().stream().mapToInt(IonMobilitySeries::getNumberOfValues).sum();
  }
}
