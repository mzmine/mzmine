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
