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
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TreeTableCell;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.jfree.chart.JFreeChart;

/**
 * Basic cell to create a chart for the feature table.
 * <br>
 * Override updateItem so that datasets are set in one call to trigger only one call to
 * {@link JFreeChart#fireChartChanged()}.
 * <br>
 * Check via {@link #isValidCell()} if a plot draw is necessary, as the first cell (id = 0) will be
 * used for measurements and does not require the full update procedure.
 */
public abstract class XyChartCell extends TreeTableCell<ModularFeatureListRow, Object> {

  private final SimpleXYChart<?> plot;
  private final Region view;
  private final int id;

  public XyChartCell(int id) {
    super();
    this.id = id;
    setMinWidth(getMinCellWidth());
    setMinHeight(getMinCellHeight());
    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

    plot = createChart();
    // use stackpane as it is transparent / borderpane is not
    view = new StackPane(plot);

    graphicProperty().bind(emptyProperty().map(empty -> empty ? null : view));
  }

  protected abstract int getMinCellHeight();

  protected abstract double getMinCellWidth();

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

  protected abstract SimpleXYChart<?> createChart();

  protected SimpleXYChart<?> getChart() {
    return plot;
  }

}
