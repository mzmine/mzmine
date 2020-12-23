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

package io.github.mzmine.gui.chartbasics.simplechart;

import com.google.common.primitives.Ints;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jfree.data.xy.XYDataset;

/**
 * Contains utility methods for {@link SimpleXYChart}.
 */
public class SimpleChartUtility {

  private SimpleChartUtility() {
  }

  /**
   * Checks if given data point is local maximum.
   *
   * @param item the index of the item to check.
   * @return true/false if the item is a local maximum.
   */
  public static boolean isLocalMaximum(XYDataset dataset, final int series, final int item) {

    final boolean isLocalMaximum;
    if (item <= 0 || item >= dataset.getItemCount(series) - 1) {

      isLocalMaximum = false;

    } else {
      final double intensity = dataset.getYValue(series, item);
      isLocalMaximum = dataset.getYValue(series, item - 1) <= intensity
          && intensity >= dataset.getYValue(series, item + 1);
    }

    return isLocalMaximum;
  }

  /**
   * Gets indexes of local maxima within given range.
   *
   * @param xMin minimum of range on x-axis.
   * @param xMax maximum of range on x-axis.
   * @param yMin minimum of range on y-axis.
   * @param yMax maximum of range on y-axis.
   * @return the local maxima in the given range.
   */
  public static int[] findLocalMaxima(XYDataset dataset, int series, final double xMin,
      final double xMax, final double yMin, final double yMax) {

    // Save data set size.
    final int currentSize = dataset.getItemCount(series);

    // If the RT values array is not filled yet, create a smaller copy.
//    if (currentSize < rtValues.length) {
//      rtCopy = new double[currentSize];
//      System.arraycopy(rtValues, 0, rtCopy, 0, currentSize);
//    } else {
//      rtCopy = rtValues;
//    }

    if (!(dataset instanceof ColoredXYDataset) || dataset.getItemCount(series) == 0) {
      return new int[0];
    }

    List<Double> xValues = ((ColoredXYDataset) dataset).getXValues();
    List<Double> yValues = ((ColoredXYDataset) dataset).getYValues();
//    int startIndex = Arrays.binarySearch(dataset.get, xMin);
    int startIndex = Collections.binarySearch(xValues, xMin);
    if (startIndex < 0) {
      startIndex = -startIndex - 1;
    }

    final int length = xValues.size();
    final Collection<Integer> indices = new ArrayList<Integer>(length);
    for (int index = startIndex; index < length && xValues.get(index) <= xMax; index++) {

      // Check Y range..
      final double intensity = yValues.get(index);
      if (yMin <= intensity && intensity <= yMax && isLocalMaximum(dataset, series, index)) {

        indices.add(index);
      }
    }

    return Ints.toArray(indices);
  }
}
