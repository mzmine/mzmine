/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.gui.chartbasics.simplechart;

import com.google.common.primitives.Ints;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.main.MZmineConfiguration;
import io.github.mzmine.main.MZmineCore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.data.xy.XYDataset;

/**
 * Contains utility methods for {@link SimpleXYChart}.
 */
public class SimpleChartUtility {

  private static final Logger logger = Logger.getLogger(SimpleChartUtility.class.getName());

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
      isLocalMaximum =
          dataset.getYValue(series, item - 1) <= intensity && intensity >= dataset.getYValue(series,
              item + 1);
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

    if (!(dataset instanceof ColoredXYDataset) || dataset.getItemCount(series) == 0) {
      return new int[0];
    }

    int startIndex = 0;
    for (int i = 0; i < dataset.getItemCount(series); i++) {
      if (dataset.getXValue(series, i) > xMin) {
        startIndex = i;
        break;
      }
    }

    if (startIndex < 0) {
      startIndex = -startIndex - 1;
    }

    final int length = dataset.getItemCount(series);
    // todo: is size = lendth correct?
    final Collection<Integer> indices = new ArrayList<>(length);
    for (int index = startIndex; index < length && dataset.getXValue(series, index) <= xMax;
        index++) {

      // Check Y range..
      final double intensity = dataset.getYValue(series, index);
      if (yMin <= intensity && intensity <= yMax && ((ColoredXYDataset) dataset).isLocalMaximum(
          index)) {
        indices.add(index);
      }
    }

    return Ints.toArray(indices);
  }

  /**
   * Applies the chart theme from the {@link MZmineConfiguration} to a renderer. This method can be
   * safely used in renderer constructors to be up-to-date, all exceptions are caught.
   */
  public static void tryApplyDefaultChartThemeToRenderer(AbstractRenderer r) {
    if (r == null) {
      return;
    }

    try {
      final MZmineConfiguration configuration = MZmineCore.getConfiguration();
      if (configuration == null) {
        logger.fine(() -> "Cannot apply item label color, configuration == null.");
        return;
      }

      final EStandardChartTheme chartTheme = configuration.getDefaultChartTheme();
      if (chartTheme == null) {
        logger.fine(() -> "Cannot apply item label color, chart theme == null.");
        return;
      }

      chartTheme.applyToAbstractRenderer(r);
    } catch (Exception e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }
  }
}
