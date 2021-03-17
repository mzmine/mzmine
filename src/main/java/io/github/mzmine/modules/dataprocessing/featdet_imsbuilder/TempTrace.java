package io.github.mzmine.modules.dataprocessing.featdet_imsbuilder;

import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.RetentionTimeMobilityDataPoint;
import java.util.HashMap;
import java.util.Map;

public class TempTrace {

  protected final Map<Integer, RetentionTimeMobilityDataPoint> datapoints = new HashMap<>();
  protected double lowestMz;
  protected double highestMz;

  public TempTrace() {

  }


  public RetentionTimeMobilityDataPoint tryToAddDataPoint(RetentionTimeMobilityDataPoint dp) {
    return datapoints.putIfAbsent(dp.getMobilityScan().getMobilityScanNumber(), dp);
  }

  public RetentionTimeMobilityDataPoint forceAddDataPoint(RetentionTimeMobilityDataPoint dp) {
    final RetentionTimeMobilityDataPoint replaced = datapoints
        .put(dp.getMobilityScan().getMobilityScanNumber(), dp);
    if (replaced == null) {

    }

  }
}
