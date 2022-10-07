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

package io.github.mzmine.modules.dataprocessing.align_ransac;

import java.text.NumberFormat;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;

import io.github.mzmine.main.MZmineCore;

/**
 * Tooltip generator
 */
class AlignmentPreviewTooltipGenerator implements XYToolTipGenerator {

  private NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
  String axis_X, axis_Y;

  public AlignmentPreviewTooltipGenerator(String axis_X, String axis_Y) {
    this.axis_X = axis_X;
    this.axis_Y = axis_Y;
  }

  /**
   * @see org.jfree.chart.labels.XYToolTipGenerator#generateToolTip(org.jfree.data.xy.XYDataset,
   *      int, int)
   */
  public String generateToolTip(XYDataset dataset, int series, int item) {

    double rtValue = dataset.getYValue(series, item);
    double rtValue2 = dataset.getXValue(series, item);

    String tooltip =
        axis_X + ": " + rtFormat.format(rtValue) + "\n" + axis_Y + ": " + rtFormat.format(rtValue2);

    return tooltip;

  }

}
