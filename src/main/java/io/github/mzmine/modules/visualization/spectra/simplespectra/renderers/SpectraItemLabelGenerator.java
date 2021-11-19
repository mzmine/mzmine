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

package io.github.mzmine.modules.visualization.spectra.simplespectra.renderers;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.util.Pair;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;

/**
 * Label generator for spectra visualizer. Only used to generate labels for the raw data
 * (ScanDataSet)
 */
public class SpectraItemLabelGenerator implements XYItemLabelGenerator {

  /*
   * Number of screen pixels to reserve for each label, so that the labels do not overlap
   */
  public static final int POINTS_RESERVE_X = 100;

  protected SpectraPlot plot;

  protected NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

  private final Map<XYDataset, List<Pair<Double, Double>>> datasetToLabelsCoords;

  public SpectraItemLabelGenerator(SpectraPlot plot) {
    this.plot = plot;
    this.datasetToLabelsCoords = plot.getDatasetToLabelsCoords();
  }

  /**
   * @see org.jfree.chart.labels.XYItemLabelGenerator#generateLabel(org.jfree.data.xy.XYDataset,
   *      int, int)
   */
  public String generateLabel(XYDataset dataset, int series, int item) {

    // X and Y values of current data point
    double originalX = dataset.getX(series, item).doubleValue();
    double originalY = dataset.getY(series, item).doubleValue();

    // Calculate data size of 1 screen pixel
    double xLength = (double) plot.getXYPlot().getDomainAxis().getRange().getLength();
    double pixelX = xLength / plot.getWidth();
    double yLength = (double) plot.getXYPlot().getRangeAxis().getRange().getLength();

    // Size of data set
    int itemCount = dataset.getItemCount(series);

    // Search for data points higher than this one in the interval
    // from limitLeft to limitRight
    double limitLeft = originalX - ((POINTS_RESERVE_X / 2) * pixelX);
    double limitRight = originalX + ((POINTS_RESERVE_X / 2) * pixelX);

    // Iterate data points to the left and right
    for (int i = 1; (item - i > 0) || (item + i < itemCount); i++) {

      // If we get out of the limit we can stop searching
      if ((item - i > 0) && (dataset.getXValue(series, item - i) < limitLeft)
          && ((item + i >= itemCount) || (dataset.getXValue(series, item + i) > limitRight)))
        break;

      if ((item + i < itemCount) && (dataset.getXValue(series, item + i) > limitRight)
          && ((item - i <= 0) || (dataset.getXValue(series, item - i) < limitLeft)))
        break;

      // If we find higher data point, bail out
      if ((item - i > 0) && (originalY <= dataset.getYValue(series, item - i)))
        return null;

      if ((item + i < itemCount) && (originalY <= dataset.getYValue(series, item + i)))
        return null;

    }

    // Avoid overlapping of labels from distinct datasets
    // Iterate over datasets
    for (XYDataset labelsDataset : datasetToLabelsCoords.keySet()) {

      // If a dataset is equal to this one, do nothing
      if (labelsDataset.equals(dataset)) {
        continue;
      }

      // If a label with coordinates close to the actual ones was already generated in another dataset,
      // do not generate a new overlapping label
      List<Pair<Double, Double>> coords = datasetToLabelsCoords.get(labelsDataset);
      for (Pair<Double, Double> coord : coords) {
        if ((Math.abs(originalX - coord.getKey()) / xLength < 0.05)
            && (Math.abs(originalY - coord.getValue()) / yLength < 0.05)) {
          return null;
        }
      }
    }

    // Update datasetToLabelsCoords if new label is to be generated
    Pair<Double, Double> newCoord = new Pair<>(originalX, originalY);
    if (datasetToLabelsCoords.get(dataset) != null) {
      datasetToLabelsCoords.get(dataset).add(newCoord);
    } else {
      datasetToLabelsCoords.put(dataset, new ArrayList<>(List.of(newCoord)));
    }

    // Create label
    String label = null;
    if (dataset instanceof ScanDataSet) {
      label = ((ScanDataSet) dataset).getAnnotation(item);
    }
    if (label == null) {
      double mzValue = dataset.getXValue(series, item);
      label = mzFormat.format(mzValue);
    }

    return label;

  }

}
