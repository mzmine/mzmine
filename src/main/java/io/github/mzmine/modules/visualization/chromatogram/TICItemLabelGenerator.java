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

package io.github.mzmine.modules.visualization.chromatogram;

import java.util.ArrayList;

import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;

import io.github.mzmine.main.MZmineCore;

/**
 * Item label generator for TIC visualizer
 * 
 * Basic method for annotation is
 * 
 * 1) Check if this data point is local maximum
 * 
 * 2) Search neighbourhood defined by pixel range for other local maxima with higher intensity
 * 
 * 3) If there is no other maximum, create a label for this one
 * 
 */
class TICItemLabelGenerator implements XYItemLabelGenerator {

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

  private TICPlot plot;

  /**
   * Constructor
   */
  TICItemLabelGenerator(TICPlot plot) {
    this.plot = plot;
  }

  /**
   * @see org.jfree.chart.labels.XYItemLabelGenerator#generateLabel(org.jfree.data.xy.XYDataset,
   *      int, int)
   */
  public String generateLabel(XYDataset dataSet, int series, int item) {

    // dataSet should be actually TICDataSet
    if (!(dataSet instanceof TICDataSet))
      return null;
    TICDataSet ticDataSet = (TICDataSet) dataSet;

    // X and Y values of current data point
    double originalX = ticDataSet.getX(0, item).doubleValue();
    double originalY = ticDataSet.getY(0, item).doubleValue();

    // Check if the intensity of this data point is above threshold
    if (originalY < ticDataSet.getMinIntensity() * THRESHOLD_FOR_ANNOTATION)
      return null;

    // Check if this data point is local maximum
    if (!ticDataSet.isLocalMaximum(item))
      return null;

    // Calculate data size of 1 screen pixel
    double xLength = (double) plot.getXYPlot().getDomainAxis().getRange().getLength();
    double pixelX = xLength / plot.getWidth();
    double yLength = (double) plot.getXYPlot().getRangeAxis().getRange().getLength();
    double pixelY = yLength / plot.getHeight();

    ArrayList<TICDataSet> allDataSets = new ArrayList<TICDataSet>();

    // Get all data sets of current plot
    for (int i = 0; i < plot.getXYPlot().getDatasetCount(); i++) {
      XYDataset dataset = plot.getXYPlot().getDataset(i);
      if (dataset instanceof TICDataSet)
        allDataSets.add((TICDataSet) dataset);
    }

    // Check each data set for conflicting data points
    for (TICDataSet checkedDataSet : allDataSets) {

      // Search for local maxima
      double searchMinX = originalX - (POINTS_RESERVE_X / 2) * pixelX;
      double searchMaxX = originalX + (POINTS_RESERVE_X / 2) * pixelX;
      double searchMinY = originalY;
      double searchMaxY = originalY + POINTS_RESERVE_Y * pixelY;

      // We don't want to search below the threshold level of the data set
      if (searchMinY < (checkedDataSet.getMinIntensity() * THRESHOLD_FOR_ANNOTATION))
        searchMinY = checkedDataSet.getMinIntensity() * THRESHOLD_FOR_ANNOTATION;

      // Do search
      int foundLocalMaxima[] =
          checkedDataSet.findLocalMaxima(searchMinX, searchMaxX, searchMinY, searchMaxY);

      // If we found other maximum then this data point, bail out
      if (foundLocalMaxima.length > (dataSet == checkedDataSet ? 1 : 0))
        return null;

    }

    // Prepare the label
    double mz = ticDataSet.getZ(0, item).doubleValue();
    String label = MZmineCore.getConfiguration().getMZFormat().format(mz);

    return label;

  }
}
