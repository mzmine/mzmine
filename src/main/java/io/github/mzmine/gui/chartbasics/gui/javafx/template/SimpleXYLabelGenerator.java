/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.gui.chartbasics.gui.javafx.template;

import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import java.util.ArrayList;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

/**
 * Default tooltip generator. Generates tooltips based on {@link io.github.mzmine.gui.chartbasics.gui.javafx.template.providers.LabelTextProvider#getLabel(int)}.
 *
 * @author https://github.com/SteffenHeu
 */
public class SimpleXYLabelGenerator implements XYItemLabelGenerator {

  /*
   * Number of screen pixels to reserve for each label, so that the labels do not overlap
   */
  public static final int POINTS_RESERVE_X = 100;
  public static final int POINTS_RESERVE_Y = 100;
  /*
   * Only data points which have intensity >= (dataset minimum value * THRESHOLD_FOR_ANNOTATION)
   * will be annotated
   */
  public static final double THRESHOLD_FOR_ANNOTATION = 2;
  private final EChartViewer chart;
  private final XYPlot plot;

  public SimpleXYLabelGenerator(final EChartViewer chart) {
    this.chart = chart;
    this.plot = chart.getChart().getXYPlot();
    if (plot == null) {
      throw new IllegalArgumentException("Char does not contain an XY-Plot.");
    }
  }

  public String generateLabel(XYDataset dataSet, int series, int item) {

    // dataSet should be actually
    if (!(dataSet instanceof ColoredXYDataset)) {
      return null;
    }
    ColoredXYDataset coloredXYDataset = (ColoredXYDataset) dataSet;

    // X and Y values of current data point
    double originalX = coloredXYDataset.getX(0, item).doubleValue();
    double originalY = coloredXYDataset.getY(0, item).doubleValue();

    // Check if the intensity of this data point is above threshold
    if (originalY
        < coloredXYDataset.getMinimumRangeValue().doubleValue() * THRESHOLD_FOR_ANNOTATION) {
      return null;
    }

    // Check if this data point is local maximum
    if (!SimpleChartUtility.isLocalMaximum(dataSet, series, item)) {
      return null;
    }

    // Calculate data size of 1 screen pixel
    double xLength = (double) plot.getDomainAxis().getRange().getLength();
    double pixelX = xLength / chart.getWidth();
    double yLength = (double) plot.getRangeAxis().getRange().getLength();
    double pixelY = yLength / chart.getHeight();

    ArrayList<ColoredXYDataset> allDataSets = new ArrayList<ColoredXYDataset>();

    // Get all data sets of current plot
    for (int i = 0; i < plot.getDatasetCount(); i++) {
      XYDataset dataset = plot.getDataset(i);
      if (dataset instanceof ColoredXYDataset) {
        allDataSets.add((ColoredXYDataset) dataset);
      }
    }

    // Check each data set for conflicting data points
    for (ColoredXYDataset checkedDataSet : allDataSets) {

      // Search for local maxima
      double searchMinX = originalX - (POINTS_RESERVE_X / 2) * pixelX;
      double searchMaxX = originalX + (POINTS_RESERVE_X / 2) * pixelX;
      double searchMinY = originalY;
      double searchMaxY = originalY + POINTS_RESERVE_Y * pixelY;

      // We don't want to search below the threshold level of the data set
      if (searchMinY < (checkedDataSet.getMinimumRangeValue() * THRESHOLD_FOR_ANNOTATION)) {
        searchMinY = checkedDataSet.getMinimumRangeValue() * THRESHOLD_FOR_ANNOTATION;
      }

      // Do search
      for (int seriesIndex = 0; seriesIndex < checkedDataSet.getSeriesCount(); seriesIndex++) {
        int foundLocalMaxima[] = SimpleChartUtility
            .findLocalMaxima(checkedDataSet, seriesIndex, searchMinX, searchMaxX, searchMinY,
                searchMaxY);
        // If we found other maximum then this data point, bail out
        if (foundLocalMaxima.length > (dataSet == checkedDataSet ? 1 : 0)) {
          return null;
        }
      }
    }

    return coloredXYDataset.getLabel(item);
  }
}
