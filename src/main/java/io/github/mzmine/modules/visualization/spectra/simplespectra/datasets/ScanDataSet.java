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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datasets;

import java.util.Map;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.IntervalXYDataset;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;

/**
 * Spectra visualizer features set for scan features points
 */
public class ScanDataSet extends AbstractXYDataset implements IntervalXYDataset {

  private static final long serialVersionUID = 1L;

  // For comparing small differences.
  private static final double EPSILON = 0.0000001;

  private String label;
  private Scan scan;
  private Map<DataPoint, String> annotation;

  /*
   * Save a local copy of m/z and intensity values, because accessing the scan every time may cause
   * reloading the features from HDD
   */
  private DataPoint dataPoints[];

  public ScanDataSet(Scan scan) {
    this("Scan #" + scan.getScanNumber(), scan);
  }

  public ScanDataSet(String label, Scan scan) {
    this.dataPoints = scan.getDataPoints();
    this.scan = scan;
    this.label = label;

    /*
     * This optimalization is disabled, because it crashes on scans with no datapoints. Also, it
     * distorts the view of the raw features - user would see something different from the actual
     * content of the raw features file.
     *
     * // remove all extra zeros List<DataPoint> dp = new ArrayList<>(); dp.add(dataPoints[0]); for
     * (int i = 1; i < dataPoints.length - 1; i++) { // previous , this and next are zero --> do not
     * add this features point if (Double.compare(dataPoints[i - 1].getIntensity(), 0d) != 0 ||
     * Double.compare(dataPoints[i].getIntensity(), 0d) != 0 || Double.compare(dataPoints[i +
     * 1].getIntensity(), 0d) != 0) { dp.add(dataPoints[i]); } } dp.add(dataPoints[dataPoints.length
     * - 1]); this.dataPoints = dp.toArray(new DataPoint[0]);
     */
  }

  @Override
  public int getSeriesCount() {
    return 1;
  }

  @Override
  public Comparable<?> getSeriesKey(int series) {
    return label;
  }

  @Override
  public int getItemCount(int series) {
    return dataPoints.length;
  }

  @Override
  public Number getX(int series, int item) {
    return dataPoints[item].getMZ();
  }

  @Override
  public Number getY(int series, int item) {
    return dataPoints[item].getIntensity();
  }

  @Override
  public Number getEndX(int series, int item) {
    return getX(series, item);
  }

  @Override
  public double getEndXValue(int series, int item) {
    return getXValue(series, item);
  }

  @Override
  public Number getEndY(int series, int item) {
    return getY(series, item);
  }

  @Override
  public double getEndYValue(int series, int item) {
    return getYValue(series, item);
  }

  @Override
  public Number getStartX(int series, int item) {
    return getX(series, item);
  }

  @Override
  public double getStartXValue(int series, int item) {
    return getXValue(series, item);
  }

  @Override
  public Number getStartY(int series, int item) {
    return getY(series, item);
  }

  @Override
  public double getStartYValue(int series, int item) {
    return getYValue(series, item);
  }

  public int getIndex(final double mz, final double intensity) {
    int index = -1;
    for (int i = 0; index < 0 && i < dataPoints.length; i++) {
      if (Math.abs(mz - dataPoints[i].getMZ()) < EPSILON
          && Math.abs(intensity - dataPoints[i].getIntensity()) < EPSILON) {

        index = i;
      }
    }
    return index;
  }

  /**
   * This function finds highest features point intensity in given m/z range. It is important for
   * normalizing isotope patterns.
   */
  public double getHighestIntensity(Range<Double> mzRange) {

    double maxIntensity = 0;
    for (DataPoint dp : dataPoints) {
      if ((mzRange.contains(dp.getMZ())) && (dp.getIntensity() > maxIntensity))
        maxIntensity = dp.getIntensity();
    }

    return maxIntensity;
  }

  public Scan getScan() {
    return scan;
  }

  public void addAnnotation(Map<DataPoint, String> annotation) {
    this.annotation = annotation;
  }

  public String getAnnotation(int item) {
    if (annotation == null)
      return null;
    DataPoint itemDataPoint = dataPoints[item];
    for (DataPoint key : annotation.keySet()) {
      if (Math.abs(key.getMZ() - itemDataPoint.getMZ()) < 0.001)
        return annotation.get(key);
    }
    return null;
  }

  public DataPoint[] getDataPoints() {
    return dataPoints;
  }
}
