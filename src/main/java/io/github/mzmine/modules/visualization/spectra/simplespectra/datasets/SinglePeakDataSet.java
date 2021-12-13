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
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.IntervalXYDataset;

/**
 * Data set for a single highlighted peak
 */
public class SinglePeakDataSet extends AbstractXYDataset implements IntervalXYDataset {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private String label;
  private double mz;
  private double intensity;


  public SinglePeakDataSet(Scan scanNumber, Feature peak) {
    this.label = peak.toString();
    if (peak instanceof ModularFeature) {
      mz = ((ModularFeature) peak).getFeatureData().getMzForSpectrum(scanNumber);
      intensity = ((ModularFeature) peak).getFeatureData().getIntensityForSpectrum(scanNumber);
    } else {
      final DataPoint dp = peak.getDataPoint(scanNumber);
      mz = dp.getMZ();
      intensity = dp.getIntensity();
    }
  }

  @Override
  public int getSeriesCount() {
    return 1;
  }

  @Override
  public Comparable<?> getSeriesKey(int series) {
    return label;
  }

  public int getItemCount(int series) {
    return 1;
  }

  public Number getX(int series, int item) {
    return mz;
  }

  public Number getY(int series, int item) {
    return intensity;
  }

  public Number getEndX(int series, int item) {
    return getX(series, item).doubleValue();
  }

  public double getEndXValue(int series, int item) {
    return getX(series, item).doubleValue();
  }

  public Number getEndY(int series, int item) {
    return getY(series, item);
  }

  public double getEndYValue(int series, int item) {
    return getYValue(series, item);
  }

  public Number getStartX(int series, int item) {
    return getX(series, item).doubleValue();
  }

  public double getStartXValue(int series, int item) {
    return getX(series, item).doubleValue();
  }

  public Number getStartY(int series, int item) {
    return getY(series, item);
  }

  public double getStartYValue(int series, int item) {
    return getYValue(series, item);
  }

}
