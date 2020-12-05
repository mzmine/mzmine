package io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder;

import com.google.common.collect.Range;
import com.google.common.math.Quantiles;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.main.MZmineCore;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Mobilogram representation. Values have to be calculated after all data points have been added.
 * Datapoints passed to this mobilogram will be stored in RAM. Use {@link
 * io.github.mzmine.project.impl.StorableMobilogram} to store data points on the disc.
 */
public class SimpleMobilogram implements Mobilogram {

  private static NumberFormat mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
  private static NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private final IMSRawDataFile rawDataFile;
  private final SortedMap<Integer, MobilityDataPoint> dataPoints;
  private final MobilityType mt;
  private double mobility;
  private double mz;
  private Range<Double> mobilityRange;
  private Range<Double> mzRange;
  private MobilityDataPoint highestDataPoint;

  public SimpleMobilogram(MobilityType mt, @Nullable IMSRawDataFile rawDataFile) {
    mobility = -1;
    mz = -1;
    dataPoints = new TreeMap<>();
    mobilityRange = null;
    mzRange = null;
    highestDataPoint = null;
    this.mt = mt;
    this.rawDataFile = rawDataFile;
  }

  public void calc() {
    mz = Quantiles.median()
        .compute(dataPoints.values().stream().map(MobilityDataPoint::getMZ).collect(
            Collectors.toList()));
    mobility = Quantiles.median()
        .compute(dataPoints.values().stream().map(MobilityDataPoint::getMobility).collect(
            Collectors.toList()));

    highestDataPoint = dataPoints.values().stream()
        .max(Comparator.comparingDouble(MobilityDataPoint::getIntensity)).get();
  }

  public boolean containsDpForScan(int scanNum) {
    return dataPoints.containsKey(scanNum);
  }

  public void addDataPoint(MobilityDataPoint dp) {
    dataPoints.put(dp.getScanNum(), dp);
    if (mobilityRange != null) {
      mobilityRange = mobilityRange.span(Range.singleton(dp.getMobility()));
      mzRange = mzRange.span(Range.singleton(dp.getMZ()));
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
    return highestDataPoint.getIntensity();
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
  public MobilityDataPoint getHighestDataPoint() {
    return highestDataPoint;
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
  public List<Number> getDomainValues() {
    return getDataPoints().stream().map(MobilityDataPoint::getMobility)
        .collect(Collectors.toList());
  }

  @Override
  public List<Number> getRangeValues() {
    return getDataPoints().stream().map(MobilityDataPoint::getIntensity)
        .collect(Collectors.toList());
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
        + " @" + mobilityFormat.format(getMobility()) + " " + getMobilityType().getUnit() + " ("
        + getDataPoints().size() + ")";
  }

  @Override
  @Nullable
  public IMSRawDataFile getRawDataFile() {
    return rawDataFile;
  }

  /**
   * @return a list of the added data points.
   */
  public List<MobilityDataPoint> fillMissingScanNumsWithZero() {
    if (dataPoints.size() <= 3) {
      return Collections.emptyList();
    }

    // find smallest mobility distance between two points
    // get two dp
    double minDist = getMobilityStepSize();

    List<MobilityDataPoint> newDps = new ArrayList<>();
    int nextScanNum = dataPoints.values().stream().findFirst().get().getScanNum() + 1;
    double lastMobility = dataPoints.get(nextScanNum - 1).getMobility();
    Iterator<MobilityDataPoint> iterator = dataPoints.values().iterator();

    if (iterator.hasNext()) {
      iterator.next();
    }
    while (iterator.hasNext()) {
      MobilityDataPoint nextDp = iterator.next();

      while (nextDp.getScanNum() != nextScanNum) {
        MobilityDataPoint newDp = new MobilityDataPoint(this.getMZ(), 0.0d,
            lastMobility - minDist, nextScanNum);
        newDps.add(newDp);
        nextScanNum++;
        lastMobility -= minDist;
      }
      lastMobility = nextDp.getMobility();
      nextScanNum = nextDp.getScanNum() + 1;
    }
    newDps.forEach(dp -> dataPoints.put(dp.getScanNum(), dp));

    calc();
    return newDps;
  }


  /**
   * Manually adds a 0 intensity data point to the edges of this mobilogram, if a gap of more than
   * minGap scans is detected.
   *
   * @param minGap
   */
  public void fillEdgesWithZeros(int minGap) {
    List<MobilityDataPoint> newDataPoints = new ArrayList<>();
    final double minStep = getMobilityStepSize();

    for (MobilityDataPoint dp : dataPoints.values()) {
      final int gap = getNumberOfConsecutiveEmptyScans(dp.getScanNum());
      if (gap > minGap) {
        MobilityDataPoint firstDp = new MobilityDataPoint(mz, 0.0, dp.getMobility() - minStep,
            dp.getScanNum() + 1);
        MobilityDataPoint lastDp = new MobilityDataPoint(mz, 0.0,
            dp.getMobility() - minStep * (gap - 1),
            dp.getScanNum() + gap - 1);
        newDataPoints.add(firstDp);
        newDataPoints.add(lastDp);
      }
    }
    newDataPoints.forEach(dp -> dataPoints.put(dp.getScanNum(), dp));
    calc();
  }

  private int getNumberOfConsecutiveEmptyScans(int startScanNum) {
    return getNextAvailableScanNumber(startScanNum) - startScanNum;
  }

  private int getNextAvailableScanNumber(int startScanNum) {
    boolean foundStartKey = false;

    for (Integer key : dataPoints.keySet()) {
      if (key == startScanNum) {
        foundStartKey = true;
        continue;
      }
      if (foundStartKey) {
        return key;
      }
    }
    return dataPoints.lastKey();
  }

  private double getMobilityStepSize() {
    // find smallest mobility distance between two points get two dp
    MobilityDataPoint aDp = null;
    for (MobilityDataPoint dp : dataPoints.values()) {
      if (aDp == null) {
        aDp = dp;
      } else {
        return Math
            .abs((aDp.getMobility() - dp.getMobility()) / (aDp.getScanNum() - dp.getScanNum()));
      }
    }
    return 0;
  }
}
