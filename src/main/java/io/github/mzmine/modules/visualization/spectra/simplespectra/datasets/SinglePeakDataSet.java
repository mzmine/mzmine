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
