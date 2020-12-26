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

package io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.ionmobilitytraceplot;

import java.util.LinkedHashSet;
import java.util.Set;
import org.jfree.data.xy.AbstractXYZDataset;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.IIonMobilityTrace;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.RetentionTimeMobilityDataPoint;

public class RetentionTimeMobilityXYZDataset extends AbstractXYZDataset {

  private Float[] xValues;
  private Double[] yValues;
  private Double[] zValues;
  private String seriesKey;

  public RetentionTimeMobilityXYZDataset(Float[] xValues, Double[] yValues, Double[] zValues,
      String seriesKey) {
    super();
    this.xValues = xValues;
    this.yValues = yValues;
    this.zValues = zValues;
    this.seriesKey = seriesKey;
  }

  public RetentionTimeMobilityXYZDataset(IIonMobilityTrace ionMobilityIonTrace) {
    seriesKey = ionMobilityIonTrace.getRepresentativeString();
    init(ionMobilityIonTrace.getDataPoints());
  }

  private void init(Set<RetentionTimeMobilityDataPoint> dataPoints) {
    Set<Float> xValuesSet = new LinkedHashSet<>();
    Set<Double> yValuesSet = new LinkedHashSet<>();
    Set<Double> zValuesSet = new LinkedHashSet<>();
    for (RetentionTimeMobilityDataPoint dataPoint : dataPoints) {
      xValuesSet.add(dataPoint.getRetentionTime());
      yValuesSet.add(dataPoint.getMobility());
      zValuesSet.add(dataPoint.getIntensity());
    }
    xValues = new Float[xValuesSet.size()];
    xValues = xValuesSet.toArray(xValues);
    yValues = new Double[yValuesSet.size()];
    yValues = yValuesSet.toArray(yValues);
    zValues = new Double[zValuesSet.size()];
    zValues = zValuesSet.toArray(zValues);
  }

  @Override
  public int getSeriesCount() {
    return 1;
  }

  @Override
  public Number getZ(int series, int item) {
    return zValues[item];
  }

  @Override
  public int getItemCount(int series) {
    return xValues.length;
  }

  @Override
  public Number getX(int series, int item) {
    return xValues[item];
  }

  @Override
  public Number getY(int series, int item) {
    return yValues[item];
  }

  @Override
  public Comparable<String> getSeriesKey(int series) {
    return seriesKey;
  }
}
