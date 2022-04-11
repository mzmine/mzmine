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

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import java.util.Vector;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.IntervalXYDataset;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;

/**
 * Picked peaks data set;
 */
public class PeakListDataSet extends AbstractXYDataset implements IntervalXYDataset {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private FeatureList featureList;

  private Feature displayedPeaks[];
  private double mzValues[], intensityValues[];
  private String label;

  public PeakListDataSet(RawDataFile dataFile, Scan scanNumber, FeatureList featureList) {
    // TODO why do we show
    this.featureList = featureList;

    Feature features[] = featureList.getFeatures(dataFile).toArray(Feature[]::new);

    Vector<Feature> candidates = new Vector<Feature>();
    for (Feature peak : features) {
      DataPoint peakDataPoint = peak.getDataPoint(scanNumber);
      if (peakDataPoint != null)
        candidates.add(peak);
    }
    displayedPeaks = candidates.toArray(new Feature[0]);

    mzValues = new double[displayedPeaks.length];
    intensityValues = new double[displayedPeaks.length];

    for (int i = 0; i < displayedPeaks.length; i++) {
      DataPoint dp = displayedPeaks[i].getDataPoint(scanNumber);
      if (dp == null)
        continue;
      mzValues[i] = dp.getMZ();
      intensityValues[i] = dp.getIntensity();
    }

    label = "Features in " + featureList.getName();

  }

  @Override
  public int getSeriesCount() {
    return 1;
  }

  @Override
  public Comparable<?> getSeriesKey(int series) {
    return label;
  }

  public FeatureList getFeatureList() {
    return featureList;
  }

  public Feature getPeak(int series, int item) {
    return displayedPeaks[item];
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
    return intensityValues[item];
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
