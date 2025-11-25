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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.StandardXYZToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYAreaRenderer2;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYBubbleRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.chart.urls.StandardXYZURLGenerator;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.chart.util.Args;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

/**
 * This is the preferred alternative to {@link ChartFactory} and generated observable versions of
 * the same charts and plots as {@link FxJFreeChart} and {@link FxXYPlot} that already come with
 * observability, optimized reduced chart draw events, and more.
 */
public class FxChartFactory {

  /**
   * Creates an area chart using an {@link XYDataset}.
   * <p>
   * The chart object returned by this method uses an {@link XYPlot} instance as the plot, with a
   * {@link NumberAxis} for the domain axis, a {@link NumberAxis} as the range axis, and a
   * {@link XYAreaRenderer} as the renderer.
   *
   * @param title      the chart title ({@code null} permitted).
   * @param xAxisLabel a label for the X-axis ({@code null} permitted).
   * @param yAxisLabel a label for the Y-axis ({@code null} permitted).
   * @param dataset    the dataset for the chart ({@code null} permitted).
   * @return An XY area chart.
   */
  public static JFreeChart createXYAreaChart(String title, String xAxisLabel, String yAxisLabel,
      XYDataset dataset) {
    return createXYAreaChart(title, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, true,
        true, false);
  }

  /**
   * Creates an area chart using an {@link XYDataset}.
   * <p>
   * The chart object returned by this method uses an {@link XYPlot} instance as the plot, with a
   * {@link NumberAxis} for the domain axis, a {@link NumberAxis} as the range axis, and a
   * {@link XYAreaRenderer} as the renderer.
   *
   * @param title       the chart title ({@code null} permitted).
   * @param xAxisLabel  a label for the X-axis ({@code null} permitted).
   * @param yAxisLabel  a label for the Y-axis ({@code null} permitted).
   * @param dataset     the dataset for the chart ({@code null} permitted).
   * @param orientation the plot orientation (horizontal or vertical) ({@code null} NOT permitted).
   * @param legend      a flag specifying whether or not a legend is required.
   * @param tooltips    configure chart to generate tool tips?
   * @param urls        configure chart to generate URLs?
   * @return An XY area chart.
   */
  public static JFreeChart createXYAreaChart(String title, String xAxisLabel, String yAxisLabel,
      XYDataset dataset, PlotOrientation orientation, boolean legend, boolean tooltips,
      boolean urls) {

    Args.nullNotPermitted(orientation, "orientation");
    NumberAxis xAxis = new NumberAxis(xAxisLabel);
    xAxis.setAutoRangeIncludesZero(false);
    NumberAxis yAxis = new NumberAxis(yAxisLabel);
    FxXYPlot plot = new FxXYPlot(dataset, xAxis, yAxis, null);
    plot.setOrientation(orientation);
    plot.setForegroundAlpha(0.5f);

    XYToolTipGenerator tipGenerator = null;
    if (tooltips) {
      tipGenerator = new StandardXYToolTipGenerator();
    }

    XYURLGenerator urlGenerator = null;
    if (urls) {
      urlGenerator = new StandardXYURLGenerator();
    }

    plot.setRenderer(new XYAreaRenderer(XYAreaRenderer.AREA, tipGenerator, urlGenerator));
    FxJFreeChart chart = new FxJFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
    ConfigService.getConfiguration().getDefaultChartTheme().apply(chart);
    return chart;

  }

  /**
   * Creates a stacked XY area plot.  The chart object returned by this method uses an
   * {@link XYPlot} instance as the plot, with a {@link NumberAxis} for the domain axis, a
   * {@link NumberAxis} as the range axis, and a {@link StackedXYAreaRenderer2} as the renderer.
   *
   * @param title      the chart title ({@code null} permitted).
   * @param xAxisLabel a label for the X-axis ({@code null} permitted).
   * @param yAxisLabel a label for the Y-axis ({@code null} permitted).
   * @param dataset    the dataset for the chart ({@code null} permitted).
   * @return A stacked XY area chart.
   */
  public static JFreeChart createStackedXYAreaChart(String title, String xAxisLabel,
      String yAxisLabel, TableXYDataset dataset) {
    return createStackedXYAreaChart(title, xAxisLabel, yAxisLabel, dataset,
        PlotOrientation.VERTICAL, true, true, false);
  }

  /**
   * Creates a stacked XY area plot.  The chart object returned by this method uses an
   * {@link XYPlot} instance as the plot, with a {@link NumberAxis} for the domain axis, a
   * {@link NumberAxis} as the range axis, and a {@link StackedXYAreaRenderer2} as the renderer.
   *
   * @param title       the chart title ({@code null} permitted).
   * @param xAxisLabel  a label for the X-axis ({@code null} permitted).
   * @param yAxisLabel  a label for the Y-axis ({@code null} permitted).
   * @param dataset     the dataset for the chart ({@code null} permitted).
   * @param orientation the plot orientation (horizontal or vertical) ({@code null} NOT permitted).
   * @param legend      a flag specifying whether or not a legend is required.
   * @param tooltips    configure chart to generate tool tips?
   * @param urls        configure chart to generate URLs?
   * @return A stacked XY area chart.
   */
  public static JFreeChart createStackedXYAreaChart(String title, String xAxisLabel,
      String yAxisLabel, TableXYDataset dataset, PlotOrientation orientation, boolean legend,
      boolean tooltips, boolean urls) {

    Args.nullNotPermitted(orientation, "orientation");
    NumberAxis xAxis = new NumberAxis(xAxisLabel);
    xAxis.setAutoRangeIncludesZero(false);
    xAxis.setLowerMargin(0.0);
    xAxis.setUpperMargin(0.0);
    NumberAxis yAxis = new NumberAxis(yAxisLabel);
    XYToolTipGenerator toolTipGenerator = null;
    if (tooltips) {
      toolTipGenerator = new StandardXYToolTipGenerator();
    }

    XYURLGenerator urlGenerator = null;
    if (urls) {
      urlGenerator = new StandardXYURLGenerator();
    }
    StackedXYAreaRenderer2 renderer = new StackedXYAreaRenderer2(toolTipGenerator, urlGenerator);
    renderer.setOutline(true);
    FxXYPlot plot = new FxXYPlot(dataset, xAxis, yAxis, renderer);
    plot.setOrientation(orientation);

    plot.setRangeAxis(yAxis);  // forces recalculation of the axis range

    FxJFreeChart chart = new FxJFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
    ConfigService.getConfiguration().getDefaultChartTheme().apply(chart);
    return chart;

  }

  /**
   * Creates a bubble chart with default settings.  The chart is composed of an {@link XYPlot}, with
   * a {@link NumberAxis} for the domain axis, a {@link NumberAxis} for the range axis, and an
   * {@link XYBubbleRenderer} to draw the data items.
   *
   * @param title      the chart title ({@code null} permitted).
   * @param xAxisLabel a label for the X-axis ({@code null} permitted).
   * @param yAxisLabel a label for the Y-axis ({@code null} permitted).
   * @param dataset    the dataset for the chart ({@code null} permitted).
   * @return A bubble chart.
   */
  public static JFreeChart createBubbleChart(String title, String xAxisLabel, String yAxisLabel,
      XYZDataset dataset) {
    return createBubbleChart(title, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, true,
        true, false);
  }

  /**
   * Creates a bubble chart with default settings.  The chart is composed of an {@link XYPlot}, with
   * a {@link NumberAxis} for the domain axis, a {@link NumberAxis} for the range axis, and an
   * {@link XYBubbleRenderer} to draw the data items.
   *
   * @param title       the chart title ({@code null} permitted).
   * @param xAxisLabel  a label for the X-axis ({@code null} permitted).
   * @param yAxisLabel  a label for the Y-axis ({@code null} permitted).
   * @param dataset     the dataset for the chart ({@code null} permitted).
   * @param orientation the orientation (horizontal or vertical) ({@code null} NOT permitted).
   * @param legend      a flag specifying whether or not a legend is required.
   * @param tooltips    configure chart to generate tool tips?
   * @param urls        configure chart to generate URLs?
   * @return A bubble chart.
   */
  public static JFreeChart createBubbleChart(String title, String xAxisLabel, String yAxisLabel,
      XYZDataset dataset, PlotOrientation orientation, boolean legend, boolean tooltips,
      boolean urls) {

    Args.nullNotPermitted(orientation, "orientation");
    NumberAxis xAxis = new NumberAxis(xAxisLabel);
    xAxis.setAutoRangeIncludesZero(false);
    NumberAxis yAxis = new NumberAxis(yAxisLabel);
    yAxis.setAutoRangeIncludesZero(false);

    FxXYPlot plot = new FxXYPlot(dataset, xAxis, yAxis, null);

    XYItemRenderer renderer = new XYBubbleRenderer(XYBubbleRenderer.SCALE_ON_RANGE_AXIS);
    if (tooltips) {
      renderer.setDefaultToolTipGenerator(new StandardXYZToolTipGenerator());
    }
    if (urls) {
      renderer.setURLGenerator(new StandardXYZURLGenerator());
    }
    plot.setRenderer(renderer);
    plot.setOrientation(orientation);

    FxJFreeChart chart = new FxJFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
    ConfigService.getConfiguration().getDefaultChartTheme().apply(chart);
    return chart;

  }

  /**
   * Creates a histogram chart.  This chart is constructed with an {@link XYPlot} using an
   * {@link XYBarRenderer}.  The domain and range axes are {@link NumberAxis} instances.
   *
   * @param title      the chart title ({@code null} permitted).
   * @param xAxisLabel the x axis label ({@code null} permitted).
   * @param yAxisLabel the y axis label ({@code null} permitted).
   * @param dataset    the dataset ({@code null} permitted).
   * @return A chart.
   */
  public static JFreeChart createHistogram(String title, String xAxisLabel, String yAxisLabel,
      IntervalXYDataset dataset) {
    return createHistogram(title, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, true,
        true, false);
  }

  /**
   * Creates a histogram chart.  This chart is constructed with an {@link XYPlot} using an
   * {@link XYBarRenderer}.  The domain and range axes are {@link NumberAxis} instances.
   *
   * @param title       the chart title ({@code null} permitted).
   * @param xAxisLabel  the x axis label ({@code null} permitted).
   * @param yAxisLabel  the y axis label ({@code null} permitted).
   * @param dataset     the dataset ({@code null} permitted).
   * @param orientation the orientation (horizontal or vertical) ({@code null} NOT permitted).
   * @param legend      create a legend?
   * @param tooltips    display tooltips?
   * @param urls        generate URLs?
   * @return The chart.
   */
  public static JFreeChart createHistogram(String title, String xAxisLabel, String yAxisLabel,
      IntervalXYDataset dataset, PlotOrientation orientation, boolean legend, boolean tooltips,
      boolean urls) {

    Args.nullNotPermitted(orientation, "orientation");
    NumberAxis xAxis = new NumberAxis(xAxisLabel);
    xAxis.setAutoRangeIncludesZero(false);
    ValueAxis yAxis = new NumberAxis(yAxisLabel);

    XYItemRenderer renderer = new XYBarRenderer();
    if (tooltips) {
      renderer.setDefaultToolTipGenerator(new StandardXYToolTipGenerator());
    }
    if (urls) {
      renderer.setURLGenerator(new StandardXYURLGenerator());
    }

    FxXYPlot plot = new FxXYPlot(dataset, xAxis, yAxis, renderer);
    plot.setOrientation(orientation);
    plot.setDomainZeroBaselineVisible(true);
    plot.setRangeZeroBaselineVisible(true);
    FxJFreeChart chart = new FxJFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
    ConfigService.getConfiguration().getDefaultChartTheme().apply(chart);
    return chart;

  }

  /**
   * Creates and returns a default instance of an XY bar chart.
   * <p>
   * The chart object returned by this method uses an {@link XYPlot} instance as the plot, with a
   * {@link DateAxis} for the domain axis, a {@link NumberAxis} as the range axis, and a
   * {@link XYBarRenderer} as the renderer.
   *
   * @param title      the chart title ({@code null} permitted).
   * @param xAxisLabel a label for the X-axis ({@code null} permitted).
   * @param dateAxis   make the domain axis display dates?
   * @param yAxisLabel a label for the Y-axis ({@code null} permitted).
   * @param dataset    the dataset for the chart ({@code null} permitted).
   * @return An XY bar chart.
   */
  public static JFreeChart createXYBarChart(String title, String xAxisLabel, boolean dateAxis,
      String yAxisLabel, IntervalXYDataset dataset) {
    return createXYBarChart(title, xAxisLabel, dateAxis, yAxisLabel, dataset,
        PlotOrientation.VERTICAL, true, true, false);
  }

  /**
   * Creates and returns a default instance of an XY bar chart.
   * <p>
   * The chart object returned by this method uses an {@link XYPlot} instance as the plot, with a
   * {@link DateAxis} for the domain axis, a {@link NumberAxis} as the range axis, and a
   * {@link XYBarRenderer} as the renderer.
   *
   * @param title       the chart title ({@code null} permitted).
   * @param xAxisLabel  a label for the X-axis ({@code null} permitted).
   * @param dateAxis    make the domain axis display dates?
   * @param yAxisLabel  a label for the Y-axis ({@code null} permitted).
   * @param dataset     the dataset for the chart ({@code null} permitted).
   * @param orientation the orientation (horizontal or vertical) ({@code null} NOT permitted).
   * @param legend      a flag specifying whether or not a legend is required.
   * @param tooltips    configure chart to generate tool tips?
   * @param urls        configure chart to generate URLs?
   * @return An XY bar chart.
   */
  public static JFreeChart createXYBarChart(String title, String xAxisLabel, boolean dateAxis,
      String yAxisLabel, IntervalXYDataset dataset, PlotOrientation orientation, boolean legend,
      boolean tooltips, boolean urls) {

    Args.nullNotPermitted(orientation, "orientation");
    ValueAxis domainAxis;
    if (dateAxis) {
      domainAxis = new DateAxis(xAxisLabel);
    } else {
      NumberAxis axis = new NumberAxis(xAxisLabel);
      axis.setAutoRangeIncludesZero(false);
      domainAxis = axis;
    }
    NumberAxis valueAxis = new NumberAxis(yAxisLabel);

    XYBarRenderer renderer = new XYBarRenderer();
    if (tooltips) {
      XYToolTipGenerator tt;
      if (dateAxis) {
        tt = StandardXYToolTipGenerator.getTimeSeriesInstance();
      } else {
        tt = new StandardXYToolTipGenerator();
      }
      renderer.setDefaultToolTipGenerator(tt);
    }
    if (urls) {
      renderer.setURLGenerator(new StandardXYURLGenerator());
    }

    FxXYPlot plot = new FxXYPlot(dataset, domainAxis, valueAxis, renderer);
    plot.setOrientation(orientation);

    FxJFreeChart chart = new FxJFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
    ConfigService.getConfiguration().getDefaultChartTheme().apply(chart);
    return chart;
  }

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
   * @param title      the chart title ({@code null} permitted).
   * @param xAxisLabel a label for the X-axis ({@code null} permitted).
   * @param yAxisLabel a label for the Y-axis ({@code null} permitted).
   * @param dataset    the dataset for the chart ({@code null} permitted).
   * @return The chart.
   */
  public static JFreeChart createXYLineChart(String title, String xAxisLabel, String yAxisLabel,
      XYDataset dataset) {
    return createXYLineChart(title, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL, true,
        true, false);
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
