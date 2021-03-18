package io.github.mzmine.modules.dataprocessing.featdet_imsbuilder;

import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.RetentionTimeMobilityDataPoint;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class TempTrace {

  private static Logger logger = Logger.getLogger(TempTrace.class.getName());

  protected final Map<Integer, RetentionTimeMobilityDataPoint> datapoints = new HashMap<>();
  protected double lowestMz;
  protected double highestMz;
  protected double centerMz;

  public TempTrace() {

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
      summedIntensities += value.getIntensity();
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
    if (currentDelta > proposedDelta) {
      return replaceDataPoint(dp);
    }
    return dp;
  }
}
