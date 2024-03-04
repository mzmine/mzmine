/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.gui.chartbasics;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

public class JFreeChartUtils {

  /**
   * Plot may contain null datasets
   *
   * @return num datasets including null
   */
  public static int getDatasetCountNullable(XYPlot plot) {
    return plot.getDatasets().size();
  }

  /**
   * Plot may contain null datasets
   *
   * @return num datasets EXCLUDING null
   */
  public static int getDatasetCountNotNull(XYPlot plot) {
    return plot.getDatasetCount();
  }

  /**
   * Plot may contain null datasets - find first index where dataset is null or return the
   * totalDataset num to append
   */
  public static int getNextDatasetIndex(XYPlot plot) {
    int totalDatasets = getDatasetCountNullable(plot);
    for (int i = 0; i < totalDatasets; i++) {
      if (plot.getDataset(i) == null) {
        return i;
      }
    }
    return totalDatasets;
  }


  /**
   * Removes all feature data sets.
   *
   * @param notify If false, the plot is not redrawn. This is useful, if multiple data sets are
   *               added right after and the plot shall not be updated until then.
   */
  public static void removeAllDataSetsOf(JFreeChart chart, Class<? extends XYDataset> clazz,
      boolean notify) {
    if (!(chart.getPlot() instanceof XYPlot plot)) {
      return;
    }

    plot.setNotify(false);
    int numDatasets = JFreeChartUtils.getDatasetCountNullable(plot);
    for (int i = 0; i < numDatasets; i++) {
      XYDataset ds = plot.getDataset(i);
      if (clazz.isInstance(ds)) {
        plot.setDataset(i, null);
        plot.setRenderer(i, null);
      }
    }
    plot.setNotify(true);
    if (notify) {
      chart.fireChartChanged();
    }
  }
}
