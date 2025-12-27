/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.features.types.graphicalnodes;

import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;

/**
 * In addition to {@link ChartCell}, this cell uses a {@link SimpleXYChart} chart and already clears
 * the plot & markers during {@link #updateItem(Object, boolean)}.
 *
 * @see ChartCell
 */
public abstract class XyChartCell extends ChartCell<SimpleXYChart<?>> {

  public XyChartCell(int id) {
    super(id);
  }

  @Override
  protected void updateItem(Object o, boolean visible) {
    // always need to call super.updateItem
    super.updateItem(o, visible);

    if (!isValidCell()) {
      return;
    }

    // remove crosshair, determined by cursor position in FxXYPlot
    plot.setCursorPosition(null);

    // clear zoom history because it comes from old data
    plot.getZoomHistory().clear();

    plot.removeAllDatasets();
  }

}
