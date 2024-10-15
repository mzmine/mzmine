/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataanalysis.rowsboxplot;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.MZmineCore;
import java.util.List;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;

public class RowsBoxplotViewBuilder extends FxViewBuilder<RowsBoxplotModel> {

  protected RowsBoxplotViewBuilder(RowsBoxplotModel model) {
    super(model);
  }

  @Override
  public Region build() {

    final NumberFormats formats = MZmineCore.getConfiguration().getGuiFormats();

    final JFreeChart barChart = ChartFactory.createBarChart("Rows box plot", "Metadata", "Height",
        null);
    ((NumberAxis) barChart.getCategoryPlot().getRangeAxis()).setNumberFormatOverride(
        formats.intensityFormat());
    final EChartViewer viewer = new EChartViewer(barChart);
    viewer.setMinWidth(200);
    final BoxAndWhiskerRenderer boxAndWhiskerRenderer = new BoxAndWhiskerRenderer();
    boxAndWhiskerRenderer.setMeanVisible(false);

    barChart.getCategoryPlot().setDataset(0, null);
    barChart.getCategoryPlot().setRenderer(0, boxAndWhiskerRenderer);

    model.datasetProperty().addListener((_, _, n) -> {
      final List<FeatureListRow> selectedRows = model.getSelectedRows();
      if (selectedRows != null && !selectedRows.isEmpty()) {
        final FeatureListRow row = selectedRows.getFirst();
        barChart.setTitle(row.toString());
      }
      barChart.getCategoryPlot().setDataset(0, n);
    });

    model.abundanceMeasureProperty().addListener((_, _, n) -> {
      barChart.getCategoryPlot().getRangeAxis().setLabel(DataTypes.get(n.type()).getHeaderString());
    });

    return new BorderPane(viewer);
  }


}
