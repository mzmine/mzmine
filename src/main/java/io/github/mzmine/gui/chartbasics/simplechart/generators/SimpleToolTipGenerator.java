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

package io.github.mzmine.gui.chartbasics.simplechart.generators;

import io.github.mzmine.gui.chartbasics.simplechart.providers.ToolTipTextProvider;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;

/**
 * Default tooltip generator. Generates tooltips based on {@link io.github.mzmine.gui.chartbasics.simplechart.providers.ToolTipTextProvider#getToolTipText(int)}.
 *
 * @author https://github.com/SteffenHeu
 */
public class SimpleToolTipGenerator implements XYToolTipGenerator {

  public SimpleToolTipGenerator() {
    super();
  }

  @Override
  public String generateToolTip(XYDataset dataset, int series, int item) {
    if (!(dataset instanceof ToolTipTextProvider)) {
      return "X: " + dataset.getX(series, item) + "\nY: " + dataset.getY(series, item);
    }
    return ((ToolTipTextProvider) dataset).getToolTipText(item);
  }
}
