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

package io.github.mzmine.modules.visualization.scatterplot.scatterplotchart;

import org.jfree.data.xy.AbstractXYDataset;

public class DiagonalLineDataset extends AbstractXYDataset {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private double min, max;
  private int fold;

  @Override
  public int getSeriesCount() {
    return 3;
  }

  @Override
  public Comparable<Integer> getSeriesKey(int series) {
    return series;
  }

  public int getItemCount(int series) {
    return 2;
  }

  public Number getX(int series, int item) {
    if (item == 0)
      return min;
    else
      return max;
  }

  public Number getY(int series, int item) {

    if (item == 0)
      switch (series) {
        case 0:
          return (min * fold);
        case 1:
          return min;
        case 2:
          return (min / fold);
      }
    else {

      switch (series) {
        case 0:
          return (max * fold);
        case 1:
          return max;
        case 2:
          return (max / fold);
      }
    }

    // We should never get here
    throw (new IllegalStateException());

  }

  public void updateDiagonalData(ScatterPlotDataSet mainDataSet, int fold) {

    this.fold = fold;

    int numOfPoints = mainDataSet.getItemCount(0);

    for (int i = 0; i < numOfPoints; i++) {

      double x = mainDataSet.getXValue(0, i);
      double y = mainDataSet.getYValue(0, i);

      if ((i == 0) || (x < min))
        min = x;
      if ((i == 0) || (x > max))
        max = x;
      if (y < min)
        min = y;
      if (y > max)
        max = y;
    }

    // Add a little space
    min -= min / 2;
    max += max / 2;

    fireDatasetChanged();
  }

  int getFold() {
    return fold;
  }

}
