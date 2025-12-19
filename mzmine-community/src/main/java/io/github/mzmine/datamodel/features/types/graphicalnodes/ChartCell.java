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

import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TreeTableCell;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public abstract class ChartCell<T extends EChartViewer> extends
    TreeTableCell<ModularFeatureListRow, Object> {

  protected final T plot;
  protected final Region view;
  protected final int id;

  public ChartCell(int id) {
    super();
    plot = createChart();
    this.id = id;
    view = new StackPane(plot);

    setMinWidth(getMinCellWidth());
    setMinHeight(getMinCellHeight());
    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

    // use stackpane as it is transparent / borderpane is not

    graphicProperty().bind(emptyProperty().map(empty -> empty ? null : view));
  }

  protected abstract int getMinCellHeight();

  protected abstract double getMinCellWidth();

  protected boolean cellHasNoData() {
    return getTableRow().getItem() == null || isEmpty();
  }

  /**
   * first cell with id 0 seems to be just the measuring cell - no need to put content. the chart
   * itself already helps measurements
   */
  protected boolean isValidCell() {
    return id != 0;
  }

  protected abstract T createChart();

  protected T getChart() {
    return plot;
  }
}
