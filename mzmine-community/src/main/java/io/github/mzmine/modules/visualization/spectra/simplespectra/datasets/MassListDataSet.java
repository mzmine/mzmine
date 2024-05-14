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
