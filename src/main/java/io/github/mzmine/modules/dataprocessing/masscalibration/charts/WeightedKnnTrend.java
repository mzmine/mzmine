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

package io.github.mzmine.modules.dataprocessing.masscalibration.charts;


import org.jfree.data.function.Function2D;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Weighted knn trend,
 * for two dimensional dataset performs weighted knn regression
 */
public class WeightedKnnTrend implements Trend2D {

  protected XYSeries dataset;
  protected XYDataItem[] items;
  protected Integer neighbors;
  protected Double neighborsFractional;

  public WeightedKnnTrend(XYSeries dataset, int neighbors) {
    this.dataset = dataset;
    this.items = (XYDataItem[]) dataset.getItems().toArray(new XYDataItem[0]);
    this.neighbors = Math.min(Math.max(neighbors, 1), items.length);
  }

  public WeightedKnnTrend(XYSeries dataset, double neighbors) {
    this(dataset, (int) Math.round(dataset.getItemCount() * neighbors));
  }

  public WeightedKnnTrend(XYSeries dataset) {
    this(dataset, 0.1);
  }

  public WeightedKnnTrend(int neighbors) {
    this.neighbors = neighbors;
    setDataset(new XYSeries("empty"));
  }

  public WeightedKnnTrend(double neighborsFractional) {
    this.neighborsFractional = neighborsFractional;
    setDataset(new XYSeries("empty"));
  }

  @Override
  public String getName() {
    return "KNN regression";
  }

  @Override
  public double getValue(double x) {
    ArrayList<XYDataItem> neighbors = findNeighbors(x);
    double arithmeticMean = neighbors.stream().mapToDouble(item -> item.getYValue()).average().orElse(0);
    return arithmeticMean;
  }

  protected ArrayList<XYDataItem> findNeighbors(double x) {
    int position = Arrays.binarySearch(items, new XYDataItem(x, 0));
    if (position < 0) {
      position = -1 * (position + 1);
    }

    int neighbors = this.neighbors != null ? this.neighbors : (int) Math.round(items.length * this.neighborsFractional);

    ArrayList<XYDataItem> closestNeighbors = new ArrayList<>();
    int lower = position;
    int upper = position + 1;

    while (closestNeighbors.size() < neighbors) {
      Double lowerNeighbor = Double.NEGATIVE_INFINITY;
      Double upperNeighbor = Double.POSITIVE_INFINITY;

      if (lower >= 0) {
        lowerNeighbor = items[lower].getXValue();
      }
      if (upper < items.length) {
        upperNeighbor = items[upper].getXValue();
      }

      if (Math.abs(x - lowerNeighbor) < Math.abs(x - upperNeighbor)) {
        closestNeighbors.add(items[lower]);
        lower--;
      } else {
        closestNeighbors.add(items[upper]);
        upper++;
      }

    }

    return closestNeighbors;
  }

  public XYSeries getDataset() {
    return dataset;
  }

  @Override
  public void setDataset(XYSeries dataset) {
    this.dataset = dataset;
    this.items = (XYDataItem[]) dataset.getItems().toArray(new XYDataItem[0]);
  }
}
