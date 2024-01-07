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

package io.github.mzmine.gui.chartbasics.simplechart.generators;

import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleChartUtility;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import java.util.ArrayList;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

/**
 * Default tooltip generator. Generates tooltips based on {@link io.github.mzmine.gui.chartbasics.simplechart.providers.LabelTextProvider#getLabel(int)}.
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
      throw new IllegalArgumentException("Chart does not contain an XY-Plot.");
    }
  }

  public String generateLabel(XYDataset dataSet, int series, int item) {

    // dataSet should be actually
    if (!(dataSet instanceof ColoredXYDataset)) {
      return null;
    }
    ColoredXYDataset coloredXYDataset = (ColoredXYDataset) dataSet;

    if (coloredXYDataset.getLabel(item) == null) { // if there is no label, we skip right away
      return null;
    }

    // X and Y values of current data point
    double originalX = coloredXYDataset.getXValue(0, item);
    double originalY = coloredXYDataset.getYValue(0, item);

    // Check if the intensity of this data point is above threshold
    if (originalY
        < (coloredXYDataset.getMinimumRangeValue().doubleValue() + 0.0001)
        * THRESHOLD_FOR_ANNOTATION) {
      return null;
    }

    // Check if this data point is local maximum
    if (!coloredXYDataset.isLocalMaximum(item)) {
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
      if (searchMinY < ((checkedDataSet.getMinimumRangeValue() + 0.0001)
          * THRESHOLD_FOR_ANNOTATION)) {
        searchMinY = (checkedDataSet.getMinimumRangeValue() + 0.0001) * THRESHOLD_FOR_ANNOTATION;
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
