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
