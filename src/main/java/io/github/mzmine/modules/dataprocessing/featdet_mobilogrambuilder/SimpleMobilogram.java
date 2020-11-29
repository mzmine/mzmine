package io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder;

import com.google.common.collect.Range;
import com.google.common.math.Quantiles;
import io.github.mzmine.datamodel.MobilityType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Mobilogram representation. Values have to be calculated after all data points have been added.
 */
public class SimpleMobilogram implements Mobilogram {

  private double mobility;
  private double mz;
  private Range<Double> mobilityRange;
  private Map<Integer, MobilityDataPoint> dataPoints;
  private final MobilityType mt;

  public SimpleMobilogram(MobilityType mt) {
    mobility = -1;
    mz = -1;
    dataPoints = new TreeMap<>();
    mobilityRange = null;
    this.mt = mt;
  }

  public void calc() {
    mz = Quantiles.median()
        .compute(dataPoints.values().stream().map(MobilityDataPoint::getMZ).collect(
            Collectors.toList()));
    mobility = Quantiles.median()
        .compute(dataPoints.values().stream().map(MobilityDataPoint::getMobility).collect(
            Collectors.toList()));
  }

  public boolean containsDpForScan(int scanNum) {
    return dataPoints.keySet().contains(scanNum);
  }

  public void addDataPoint(MobilityDataPoint dp) {
    dataPoints.put(dp.getScanNum(), dp);
    if(mobilityRange != null) {
      mobilityRange.span(Range.singleton(dp.getMobility()));
    } else {
      mobilityRange = Range.singleton(dp.getMobility());
    }
  }

  /**
   * Make sure {@link SimpleMobilogram#calc()} has been called
   * @return the median mz
   */
  @Override
  public double getMZ() {
    return mz;
  }

  /**
   * Make sure {@link SimpleMobilogram#calc()} has been called
   * @return the median mobility
   */
  @Override
  public double getMobility() {
    return mobility;
  }

  @Override
  public Range<Double> getMobilityRange() {
    return mobilityRange;
  }

  @Override
  public List<MobilityDataPoint> getDataPoints() {
    return new ArrayList<>(dataPoints.values());
  }

  @Override
  public List<Integer> getScanNumbers() {
    return new ArrayList<>(dataPoints.keySet());
  }

  @Override
  public MobilityType getMobilityType() {
    return mt;
  }
}
