/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.util.reporting.jasper;

import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataanalysis.rowsboxplot.RowBoxPlotDataset;
import io.github.mzmine.util.color.SimpleColorPalette;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;

public class ExportBoxplot extends EChartViewer {

  private final BoxAndWhiskerRenderer boxAndWhiskerRenderer;

  public ExportBoxplot() {
    final NumberFormats formats = MZmineCore.getConfiguration().getGuiFormats();
    final JFreeChart barChart = ChartFactory.createBarChart("Rows box plot", "Metadata", "Height",
        null);
    ((NumberAxis) barChart.getCategoryPlot().getRangeAxis()).setNumberFormatOverride(
        formats.intensityFormat());

    super(barChart);

    setMinWidth(200);
    boxAndWhiskerRenderer = new BoxAndWhiskerRenderer();
    boxAndWhiskerRenderer.setMeanVisible(false);

    barChart.getCategoryPlot().setDataset(0, null);
    barChart.getCategoryPlot().setRenderer(0, boxAndWhiskerRenderer);

    barChart.getCategoryPlot().getRangeAxis().setLabel("Area");
    barChart.setTitle((String) null);

    barChart.getCategoryPlot().getDomainAxis().setTickLabelsVisible(false);
    barChart.getCategoryPlot().getDomainAxis().setTickMarksVisible(false);

    ((NumberAxis) getChart().getCategoryPlot()
        .getRangeAxis()).setNumberFormatOverride(ConfigService.getGuiFormats().intensityFormat());
  }

  public void setDataset(RowBoxPlotDataset ds) {
    applyWithNotifyChanges(false, () -> {
      getChart().getCategoryPlot().setDataset(0, ds);
      final SimpleColorPalette colors = ds.getColorPalette();
      colors.applyToChart(getChart());
      // need to reset the renderer otherwise the old will keep the colors
      boxAndWhiskerRenderer.clearSeriesPaints(false);
      boxAndWhiskerRenderer.clearSeriesStrokes(false);
    });
  }
}
