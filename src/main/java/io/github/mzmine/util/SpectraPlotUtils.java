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

package io.github.mzmine.util;

import java.util.List;
import org.jfree.data.xy.XYDataset;

import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;

public class SpectraPlotUtils {

  /**
   * Removes all label generators of datasets that are not of the given type.
   * 
   * @param plot Plot to apply this method to.
   * @param ignore List of class objects of the instances to ignore.
   */
  public static void clearDatasetLabelGenerators(SpectraPlot plot,
      List<Class<? extends XYDataset>> ignore) {
    for (int i = 0; i < plot.getXYPlot().getDatasetCount(); i++) {
      XYDataset dataset = plot.getXYPlot().getDataset(i);
      // check if object of dataset is an instance of ignore.class
      boolean remove = true;
      for (Class<? extends XYDataset> datasetClass : ignore) {
        if ((datasetClass.isInstance(dataset)))
          remove = false;
      }

      if (remove)
        plot.getXYPlot().getRendererForDataset(dataset).setDefaultItemLabelGenerator(null);
    }
  }

  /**
   * Removes all label generators of datasets that are not of the given type.
   * 
   * @param plot Plot to apply this method to.
   * @param ignore Class object of the instances to ignore.
   */
  public static void clearDatasetLabelGenerators(SpectraPlot plot,
      Class<? extends XYDataset> ignore) {
    for (int i = 0; i < plot.getXYPlot().getDatasetCount(); i++) {
      XYDataset dataset = plot.getXYPlot().getDataset(i);
      // check if object of dataset is an instance of ignore.class
      if (!(ignore.isInstance(dataset)))
        plot.getXYPlot().getRendererForDataset(dataset).setDefaultItemLabelGenerator(null);
    }
  }
}
