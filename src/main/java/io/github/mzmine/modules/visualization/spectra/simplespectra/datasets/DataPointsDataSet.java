/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.spectra.simplespectra.datasets;

import io.github.mzmine.datamodel.DataPoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.IntervalXYDataset;

/**
 * Data set for MzPeaks, used in feature detection preview
 */
public class DataPointsDataSet extends AbstractXYDataset implements IntervalXYDataset,
    RelativeOption {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private final double maxIntensity;
  protected DataPoint[] mzPeaks;
  private boolean normalize;
  private String label;

  public DataPointsDataSet(String label, DataPoint mzPeaks[]) {
    this(label, mzPeaks, false);
  }

  public DataPointsDataSet(String label, DataPoint mzPeaks[], boolean normalize) {
    this.label = label;
    this.mzPeaks = mzPeaks;
    this.normalize = normalize;
    // if we have some data points, remove extra zeros
    if (mzPeaks.length > 0) {
      List<DataPoint> dp = new ArrayList<>();
      dp.add(mzPeaks[0]);
      for (int i = 1; i < mzPeaks.length - 1; i++) {
        // previous , this and next are zero --> do not add this data
        // point
        if (Double.compare(mzPeaks[i - 1].getIntensity(), 0d) != 0
            || Double.compare(mzPeaks[i].getIntensity(), 0d) != 0
            || Double.compare(mzPeaks[i + 1].getIntensity(), 0d) != 0) {
          dp.add(mzPeaks[i]);
        }

        dp.add(mzPeaks[mzPeaks.length - 1]);
      }
      this.mzPeaks = dp.toArray(new DataPoint[0]);
    }

    maxIntensity = Arrays.stream(mzPeaks).mapToDouble(DataPoint::getIntensity).max().orElse(1);
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
    return mzPeaks.length;
  }

  @Override
  public Number getX(int series, int item) {
    return mzPeaks[item].getMZ();
  }

  @Override
  public Number getY(int series, int item) {
    return normalize ? mzPeaks[item].getIntensity() / maxIntensity * 100d
        : mzPeaks[item].getIntensity();
  }

  @Override
  public Number getEndX(int series, int item) {
    return getX(series, item).doubleValue();
  }

  @Override
  public double getEndXValue(int series, int item) {
    return getX(series, item).doubleValue();
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
    return getX(series, item).doubleValue();
  }

  @Override
  public double getStartXValue(int series, int item) {
    return getX(series, item).doubleValue();
  }

  @Override
  public Number getStartY(int series, int item) {
    return getY(series, item);
  }

  @Override
  public double getStartYValue(int series, int item) {
    return getYValue(series, item);
  }

  @Override
  public void setRelative(boolean relative) {
    normalize = relative;
  }
}
