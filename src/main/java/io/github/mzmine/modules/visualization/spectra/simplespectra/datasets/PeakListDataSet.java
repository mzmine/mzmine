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
