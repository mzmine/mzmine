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
