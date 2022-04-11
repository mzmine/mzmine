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
import java.util.List;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.IntervalXYDataset;

/**
 * Data set for MzPeaks, used in feature detection preview
 */
public class MzIntensityDataSet extends AbstractXYDataset implements IntervalXYDataset {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  protected double[] mzs;
  protected double[] intensities;
  private String label;

  public MzIntensityDataSet(String label, double[] mzs, double[] intensities) {
    this.label = label;
    this.mzs = mzs;
    this.intensities = intensities;
    assert mzs.length == intensities.length;
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
    return mzs.length;
  }

  @Override
  public Number getX(int series, int item) {
    return mzs[item];
  }

  @Override
  public Number getY(int series, int item) {
    return intensities[item];
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

}
