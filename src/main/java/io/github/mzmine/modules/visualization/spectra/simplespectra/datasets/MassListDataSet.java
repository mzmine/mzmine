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

import io.github.mzmine.datamodel.MassList;
import java.util.Arrays;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.IntervalXYDataset;

/**
 * Data set for MassList. Implements IntervalXYDataset to be used in pair with XYBarRenderer.
 */
public class MassListDataSet extends AbstractXYDataset implements IntervalXYDataset,
    RelativeOption {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private final double maxIntensity;
  private final double mzValues[], intensityValues[];
  private boolean normalize;

  public MassListDataSet(MassList massList) {
    this(massList, false);
  }

  public MassListDataSet(MassList massList, boolean normalize) {
    this.mzValues = new double[massList.getNumberOfDataPoints()];
    this.intensityValues = new double[massList.getNumberOfDataPoints()];
    massList.getMzValues(this.mzValues);
    massList.getIntensityValues(this.intensityValues);
    maxIntensity = Arrays.stream(intensityValues).max().orElse(1d);
    this.normalize = normalize;
  }

  public MassListDataSet(double mzValues[], double intensityValues[]) {
    this(mzValues, intensityValues, false);
  }

  public MassListDataSet(double mzValues[], double intensityValues[], boolean normalize) {
    this.mzValues = mzValues;
    this.intensityValues = intensityValues;
    maxIntensity = Arrays.stream(intensityValues).max().orElse(1d);
    this.normalize = normalize;
  }

  @Override
  public int getSeriesCount() {
    return 1;
  }

  @Override
  public Comparable getSeriesKey(int series) {
    return 1;
  }

  @Override
  public int getItemCount(int series) {
    return mzValues.length;
  }

  @Override
  public Number getX(int series, int item) {
    return mzValues[item];
  }

  @Override
  public Number getY(int series, int item) {
    return normalize ? intensityValues[item] / maxIntensity * 100d : intensityValues[item];
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

  @Override
  public void setRelative(boolean relative) {
    normalize = relative;
  }
}
