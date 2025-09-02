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

import io.github.mzmine.gui.chartbasics.gui.javafx.model.FxJFreeChart;
import io.github.mzmine.gui.chartbasics.gui.javafx.model.FxXYPlot;
import io.github.mzmine.main.ConfigService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.xy.XYDataset;

public class FxChartFactory {

  /**
   * Creates a scatter plot with default settings.  The chart object returned by this method uses an
   * {@link XYPlot} instance as the plot, with a {@link NumberAxis} for the domain axis, a
   * {@link NumberAxis} as the range axis, and an {@link XYLineAndShapeRenderer} as the renderer.
   *
   * @param title       the chart title ({@code null} permitted).
   * @param xAxisLabel  a label for the X-axis ({@code null} permitted).
   * @param yAxisLabel  a label for the Y-axis ({@code null} permitted).
   * @param dataset     the dataset for the chart ({@code null} permitted).
   * @param orientation the plot orientation (horizontal or vertical) ({@code null} NOT permitted).
   * @param legend      a flag specifying whether or not a legend is required.
   * @param tooltips    configure chart to generate tool tips?
   * @param urls        configure chart to generate URLs?
   * @return A scatter plot.
   */
  public static FxJFreeChart createScatterPlot(String title, String xAxisLabel, String yAxisLabel,
      XYDataset dataset, @NotNull PlotOrientation orientation, boolean legend, boolean tooltips,
      boolean urls) {

    NumberAxis xAxis = new NumberAxis(xAxisLabel);
    xAxis.setAutoRangeIncludesZero(false);
    NumberAxis yAxis = new NumberAxis(yAxisLabel);
    yAxis.setAutoRangeIncludesZero(false);

    FxXYPlot plot = new FxXYPlot(dataset, xAxis, yAxis, null);

    XYToolTipGenerator toolTipGenerator = null;
    if (tooltips) {
      toolTipGenerator = new StandardXYToolTipGenerator();
    }

    XYURLGenerator urlGenerator = null;
    if (urls) {
      urlGenerator = new StandardXYURLGenerator();
    }
    XYItemRenderer renderer = new XYLineAndShapeRenderer(false, true);
    renderer.setDefaultToolTipGenerator(toolTipGenerator);
    renderer.setURLGenerator(urlGenerator);
    plot.setRenderer(renderer);
    plot.setOrientation(orientation);

    FxJFreeChart chart = new FxJFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
    ConfigService.getConfiguration().getDefaultChartTheme().apply(chart);
    return chart;

  }

  /**
   * Creates a line chart (based on an {@link XYDataset}) with default settings.
   *
   * @param title       the chart title ({@code null} permitted).
   * @param xAxisLabel  a label for the X-axis ({@code null} permitted).
   * @param yAxisLabel  a label for the Y-axis ({@code null} permitted).
   * @param dataset     the dataset for the chart ({@code null} permitted).
   * @param orientation the plot orientation (horizontal or vertical) ({@code null} NOT permitted).
   * @param legend      a flag specifying whether or not a legend is required.
   * @param tooltips    configure chart to generate tool tips?
   * @return The chart.
   */
  public static FxJFreeChart createXYLineChart(String title, String xAxisLabel, String yAxisLabel,
      @Nullable XYDataset dataset, @NotNull PlotOrientation orientation, boolean legend,
      boolean tooltips, boolean urls) {

    NumberAxis xAxis = new NumberAxis(xAxisLabel);
    xAxis.setAutoRangeIncludesZero(false);
    NumberAxis yAxis = new NumberAxis(yAxisLabel);
    XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
    FxXYPlot plot = new FxXYPlot(dataset, xAxis, yAxis, renderer);
    plot.setOrientation(orientation);
    if (tooltips) {
      renderer.setDefaultToolTipGenerator(new StandardXYToolTipGenerator());
    }
    if (urls) {
      renderer.setURLGenerator(new StandardXYURLGenerator());
    }

    FxJFreeChart chart = new FxJFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
    ConfigService.getConfiguration().getDefaultChartTheme().apply(chart);
    return chart;
  }

}
