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

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.ArrayList;
import org.jfree.data.xy.AbstractXYDataset;
import io.github.mzmine.modules.visualization.scatterplot.ScatterPlotAxisSelection;
import io.github.mzmine.util.SearchDefinition;

/**
 *
 * This data set contains 2 series: first series (index 0) contains all feature list rows. Second
 * series (index 1) contains those feature list rows which conform to current search definition
 * (currentSearch).
 *
 */
public class ScatterPlotDataSet extends AbstractXYDataset {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  private FeatureListRow displayedRows[], selectedRows[];

  private ScatterPlotAxisSelection axisX, axisY;
  private SearchDefinition currentSearch;

  // We use this value for zero data points, because zero cannot be plotted in
  // the log-scale scatter plot
  private double defaultValue;

  public ScatterPlotDataSet(FeatureList featureList) {
    this.displayedRows = featureList.getRows().toArray(FeatureListRow[]::new);
  }

  void setDisplayedAxes(ScatterPlotAxisSelection axisX, ScatterPlotAxisSelection axisY) {

    this.axisX = axisX;
    this.axisY = axisY;

    // Update the default value to minimum value divided by 2
    double minValue = Double.MAX_VALUE;
    for (FeatureListRow row : displayedRows) {
      double valX = axisX.getValue(row);
      double valY = axisX.getValue(row);
      if ((valX > 0) && (valX < minValue))
        minValue = valX;
      if ((valY > 0) && (valY < minValue))
        minValue = valY;
    }
    this.defaultValue = minValue / 2;

    updateSearchDefinition(currentSearch);

  }

  public FeatureListRow getRow(int series, int item) {
    if (series == 0)
      return displayedRows[item];
    else
      return selectedRows[item];
  }

  @Override
  public int getSeriesCount() {
    if ((displayedRows == null) || (axisX == null) || (axisY == null))
      return 0;
    if ((selectedRows == null) || (selectedRows.length == 0))
      return 1;
    return 2;
  }

  @Override
  public Comparable<Integer> getSeriesKey(int series) {
    return series;
  }

  /**
   *
   */
  @Override
  public int getItemCount(int series) {
    if (series == 0)
      return displayedRows.length;
    else
      return selectedRows.length;
  }

  /**
   *
   */
  @Override
  public Number getX(int series, int item) {
    double value;
    if (series == 0)
      value = axisX.getValue(displayedRows[item]);
    else
      value = axisX.getValue(selectedRows[item]);
    // We must not return zero, because it cannot be plotted
    if (value > 0)
      return value;
    else
      return defaultValue;
  }

  /**
   *
   */
  @Override
  public Number getY(int series, int item) {
    double value;
    if (series == 0)
      value = axisY.getValue(displayedRows[item]);
    else
      value = axisY.getValue(selectedRows[item]);
    // We must not return zero, because it cannot be plotted
    if (value > 0)
      return value;
    else
      return defaultValue;
  }

  /**
   * Returns the feature list row which exactly matches given X and Y values
   */
  public FeatureListRow getRow(double valueX, double valueY) {

    for (int i = 0; i < displayedRows.length; i++) {
      if ((Math.abs(valueX - getXValue(0, i)) < 0.0000001)
          && (Math.abs(valueY - getYValue(0, i)) < 0.0000001))
        return displayedRows[i];
    }
    return null;
  }

  void updateSearchDefinition(SearchDefinition newSearch) {

    this.currentSearch = newSearch;

    if (newSearch == null) {
      this.selectedRows = new FeatureListRow[0];
    } else {
      ArrayList<FeatureListRow> selected = new ArrayList<FeatureListRow>();
      for (FeatureListRow row : displayedRows) {
        if (newSearch.conforms(row)) {
          selected.add(row);
        }
      }
      this.selectedRows = selected.toArray(new FeatureListRow[0]);
    }

    fireDatasetChanged();
  }

}
