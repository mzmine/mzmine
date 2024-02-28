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

package io.github.mzmine.modules.dataprocessing.featdet_masscalibration.charts;

import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Arithmetic mean knn trend,
 * for two dimensional dataset performs knn regression by taking arithmetic mean of neighbors
 * <p>
 * it is possible to largely optimize this class by precomputing the predicted trend values
 * since dataset is not changed often, but getValue() is used many times (for each mass peak for each scan
 * when running mass calibration), precomputing the values and keeping track of all the points where
 * the mean of k closest neighbors changes could give a sizeable boost
 * <p>
 * it currently precomputes the values evenly distributed with fixed resolution across the range of x values
 * it then uses linear interpolation between two closest precomputed points to approximate the knn trend value for
 * required argument value
 * the approximation can be off considerably at few points as it will often decrease local variance
 * (for instance the nearly vertical strands of points coming from similar m/z values at close RT matched with the
 * same calibrant)
 * and make the approximate trend version continuous with the interpolation between the points
 * with sufficient resolution the approximation can get as close as needed to the actual trend
 */
public class ArithmeticMeanKnnTrend implements Trend2D {

  protected XYSeries dataset;
  protected XYDataItem[] items;
  protected Integer neighbors;
  protected Double neighborsFractional;

  protected Double rSquared;

  protected int resolution = 10_001;
  protected boolean usePrecomputedApproximation = true;
  protected Double[] precomputedPoints;
  double smallestX;
  double largestX;
  double rangeX;
  double deltaX;

  public ArithmeticMeanKnnTrend(XYSeries dataset, int neighbors) {
    this.dataset = dataset;
    this.items = (XYDataItem[]) dataset.getItems().toArray(new XYDataItem[0]);
    this.neighbors = Math.min(Math.max(neighbors, 1), items.length);
  }

  public ArithmeticMeanKnnTrend(XYSeries dataset, double neighbors) {
    this(dataset, (int) Math.round(dataset.getItemCount() * neighbors));
  }

  public ArithmeticMeanKnnTrend(XYSeries dataset) {
    this(dataset, 0.1);
  }

  public ArithmeticMeanKnnTrend(int neighbors) {
    this.neighbors = neighbors;
    setDataset(new XYSeries("empty"));
  }

  public ArithmeticMeanKnnTrend(double neighborsFractional) {
    this.neighborsFractional = neighborsFractional;
    setDataset(new XYSeries("empty"));
  }

  @Override
  public String getName() {
    return "KNN regression\nR^2 = " + rSquared;
  }

  @Override
  public double getValue(double x) {
    if (items.length == 0) {
      return 0;
    }
    return usePrecomputedApproximation ? getValuePrecomputed(x) : getValueDirect(x);
  }

  public double getValuePrecomputed(double x) {
    if (x <= smallestX) {
      return precomputedPoints[0];
    }
    if (x >= largestX) {
      return precomputedPoints[precomputedPoints.length - 1];
    }

    double offsetX = x - smallestX;
    int pointOffset = (int) Math.floor(offsetX / deltaX);
    if (pointOffset == precomputedPoints.length - 1) {
      return precomputedPoints[pointOffset];
    }
    int pointOffsetNext = pointOffset + 1;
    double x1 = smallestX + pointOffset * deltaX;
    double y1 = precomputedPoints[pointOffset];
    double x2 = smallestX + pointOffsetNext * deltaX;
    double y2 = precomputedPoints[pointOffsetNext];
    return interpolateLinearly(x1, y1, x2, y2, x);
  }

  public double getValueDirect(double x) {
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
    int lower = position - (position == items.length ? 1 : 0);
    int upper = lower + 1;

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

  protected double interpolateLinearly(double x1, double y1, double x2, double y2, double x) {
    double fractionalX = (x - x1) / (x2 - x1);
    double y = fractionalX * (y2 - y1) + y1;
    return y;
  }

  protected void precomputeValues() {
    smallestX = items[0].getXValue();
    largestX = items[items.length - 1].getXValue();
    rangeX = largestX - smallestX;
    deltaX = rangeX / (resolution - 1);
    precomputedPoints = new Double[resolution];
    for (int i = 0; i < precomputedPoints.length; i++) {
      double x = smallestX + deltaX * i;
      precomputedPoints[i] = getValueDirect(x);
    }
  }

  public XYSeries getDataset() {
    return dataset;
  }

  @Override
  public void setDataset(XYSeries dataset) {
    this.dataset = dataset;
    this.items = (XYDataItem[]) dataset.getItems().toArray(new XYDataItem[0]);
    Arrays.sort(items);
    if (items.length > 0) {
      precomputeValues();
      rSquared = ChartUtils.calculateRSquared(items, this);
    } else {
      rSquared = null;
    }
  }
}
