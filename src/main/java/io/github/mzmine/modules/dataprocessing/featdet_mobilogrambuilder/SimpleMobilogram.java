package io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder;

import com.google.common.collect.Range;
import com.google.common.math.Quantiles;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.main.MZmineCore;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * Mobilogram representation. Values have to be calculated after all data points have been added.
 */
public class SimpleMobilogram implements Mobilogram {

  private static NumberFormat mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
  private static NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private final SortedMap<Integer, MobilityDataPoint> dataPoints;
  private final MobilityType mt;
  private double mobility;
  private double mz;
  private double maximumIntensity;
  private Range<Double> mobilityRange;
  private Range<Double> mzRange;

  public SimpleMobilogram(MobilityType mt) {
    mobility = -1;
    mz = -1;
    maximumIntensity = -1;
    dataPoints = new TreeMap<>();
    mobilityRange = null;
    mzRange = null;
    this.mt = mt;
  }

  public void calc() {
    mz = Quantiles.median()
        .compute(dataPoints.values().stream().map(MobilityDataPoint::getMZ).collect(
            Collectors.toList()));
    mobility = Quantiles.median()
        .compute(dataPoints.values().stream().map(MobilityDataPoint::getMobility).collect(
            Collectors.toList()));

    maximumIntensity =
        dataPoints.values().stream().mapToDouble(MobilityDataPoint::getIntensity).max()
            .getAsDouble();
  }

  public boolean containsDpForScan(int scanNum) {
    return dataPoints.containsKey(scanNum);
  }

  public void addDataPoint(MobilityDataPoint dp) {
    dataPoints.put(dp.getScanNum(), dp);
    if (mobilityRange != null) {
      mobilityRange.span(Range.singleton(dp.getMobility()));
      mzRange.span(Range.singleton(dp.getMZ()));
    } else {
      mobilityRange = Range.singleton(dp.getMobility());
      mzRange = Range.singleton(dp.getMZ());
    }
  }

  /**
   * Make sure {@link SimpleMobilogram#calc()} has been called
   *
   * @return the median mz
   */
  @Override
  public double getMZ() {
    return mz;
  }

  /**
   * Make sure {@link SimpleMobilogram#calc()} has been called
   *
   * @return the median mobility
   */
  @Override
  public double getMobility() {
    return mobility;
  }

  @Override
  public double getMaximumIntensity() {
    return maximumIntensity;
  }

  @Override
  public Range<Double> getMZRange() {
    return mzRange;
  }

  @Override
  public Range<Double> getMobilityRange() {
    return mobilityRange;
  }

  @Nonnull
  @Override
  public List<MobilityDataPoint> getDataPoints() {
    return new ArrayList<>(dataPoints.values());
  }

  @Nonnull
  @Override
  public List<Integer> getScanNumbers() {
    return new ArrayList<>(dataPoints.keySet());
  }

  @Override
  public MobilityType getMobilityType() {
    return mt;
  }

  @Override
  public Color getAWTColor() {
    return Color.black;
  }

  @Override
  public javafx.scene.paint.Color getFXColor() {
    return javafx.scene.paint.Color.BLACK;
  }

  @Override
  public Number getDomainValue(int index) {
    return getDataPoints().get(index).getMobility();
  }

  @Override
  public Number getRangeValue(int index) {
    return getDataPoints().get(index).getIntensity();
  }

  @Override
  public Comparable<?> getSeriesKey() {
    return "m/z range " + getMZRange().toString();
  }

  @Override
  public int getValueCount() {
    return getDataPoints().size();
  }

  @Override
  public String representativeString() {
    return mzFormat.format(mzRange.lowerEndpoint()) + " - " + mzFormat
        .format(mzRange.upperEndpoint())
        + " @" + mobilityFormat.format(getMobility()) + " " + getMobilityType().getUnit();
  }
}
