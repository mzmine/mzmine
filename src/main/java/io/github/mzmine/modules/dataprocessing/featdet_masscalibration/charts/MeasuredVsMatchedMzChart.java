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

package io.github.mzmine.modules.dataprocessing.featdet_masscalibration.charts;


import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.MassPeakMatch;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.List;

/**
 * Chart for measured mz vs matched mz plots (xy scatter plot of measured mz vs matched mz)
 */
public class MeasuredVsMatchedMzChart extends EChartViewer {

  private final JFreeChart chart;

  protected MeasuredVsMatchedMzChart(JFreeChart chart) {
    super(chart);
    this.chart = chart;
  }

  public MeasuredVsMatchedMzChart(String title) {
    this(createEmptyChart(title));
  }

  public MeasuredVsMatchedMzChart() {
    this("Measured mz vs matched mz");
  }

  public static JFreeChart createEmptyChart(String title) {
    XYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES);
    NumberAxis xAxis = new NumberAxis("Matched mz");
    NumberAxis yAxis = new NumberAxis("Measured mz");
    XYPlot plot = new XYPlot(null, xAxis, yAxis, renderer);
    plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);

//    JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
    JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
    chart.setTitle((String) null);
    return chart;
  }

  public void cleanPlot() {
    XYPlot plot = chart.getXYPlot();
    ChartUtils.cleanPlot(plot);
  }

  public void updatePlot(List<MassPeakMatch> matches) {
    XYPlot plot = chart.getXYPlot();

    XYDataset dataset = createChartDataset(matches);
    plot.setDataset(dataset);
  }

  protected XYDataset createChartDataset(List<MassPeakMatch> matches) {
    XYSeries errorsXY = new XYSeries("Mz matches");
    for (MassPeakMatch match : matches) {
      errorsXY.add(match.getMatchedMzRatio(), match.getMeasuredMzRatio());
    }

    return new XYSeriesCollection(errorsXY);
  }

}
