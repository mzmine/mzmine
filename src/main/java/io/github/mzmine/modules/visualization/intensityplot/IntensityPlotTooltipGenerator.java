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

package io.github.mzmine.modules.visualization.intensityplot;

import java.text.Format;

import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.XYDataset;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;

/**
 * 
 */
class IntensityPlotTooltipGenerator implements CategoryToolTipGenerator, XYToolTipGenerator {

  /**
   * @see org.jfree.chart.labels.CategoryToolTipGenerator#generateToolTip(org.jfree.data.category.CategoryDataset,
   *      int, int)
   */
  public String generateToolTip(CategoryDataset dataset, int row, int column) {
    Format intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    Feature features[] = ((IntensityPlotDataset) dataset).getFeatures(row, column);
    RawDataFile files[] = ((IntensityPlotDataset) dataset).getFiles(column);

    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < files.length; i++) {
      sb.append(files[i].getName());
      sb.append(": ");
      if (features[i] != null) {
        sb.append(features[i].toString());
        sb.append(", height: ");
        sb.append(intensityFormat.format(features[i].getHeight()));
      } else {
        sb.append("N/A");
      }
      sb.append("\n");
    }

    return sb.toString();
  }

  public String generateToolTip(XYDataset dataset, int series, int item) {
    return generateToolTip((CategoryDataset) dataset, series, item);
  }

}
