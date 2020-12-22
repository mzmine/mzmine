/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder;

import com.google.common.collect.Range;
import com.google.common.math.Quantiles;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.main.MZmineCore;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
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
  public List<Integer> getMobilityScanNumbers() {
    return new ArrayList<>(dataPoints.keySet());
  }

  @Override
  public MobilityType getMobilityType() {
    return mt;
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
}
