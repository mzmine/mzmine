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

package io.github.mzmine.modules.dataprocessing.adap_mcr;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import com.google.common.collect.Range;
import dulab.adap.datamodel.BetterPeak;
import dulab.adap.workflow.decomposition.RetTimeClusterer;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import javafx.scene.Cursor;

/**
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */

public class SimpleScatterPlot extends EChartViewer {
  private final XYPlot plot;
  private final XYSeriesCollection xyDataset;

  SimpleScatterPlot(String xLabel, String yLabel) {
    super(null);

    // setBackground(Color.white);
    setCursor(Cursor.CROSSHAIR);

    NumberAxis xAxis = new NumberAxis(xLabel);
    xAxis.setAutoRangeIncludesZero(false);
    xAxis.setUpperMargin(0);
    xAxis.setLowerMargin(0);

    NumberAxis yAxis = new NumberAxis(yLabel);
    yAxis.setAutoRangeIncludesZero(false);
    yAxis.setUpperMargin(0);
    yAxis.setLowerMargin(0);

    xyDataset = new XYSeriesCollection();

    XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false) {
      @Override
      protected void drawPrimaryLine(XYItemRendererState state, Graphics2D g2, XYPlot plot,
          XYDataset dataset, int pass, int series, int item, ValueAxis domainAxis,
          ValueAxis rangeAxis, Rectangle2D dataArea) {
        if (item % 2 != 0)
          super.drawPrimaryLine(state, g2, plot, dataset, pass, series, item, domainAxis, rangeAxis,
              dataArea);
      }
    };

    plot = new XYPlot(xyDataset, xAxis, yAxis, renderer);
    plot.setBackgroundPaint(Color.white);
    plot.setDomainGridlinesVisible(true);
    plot.setRangeGridlinesVisible(true);

    JFreeChart chart = new JFreeChart("", new Font("SansSerif", Font.BOLD, 12), plot, false);
    chart.setBackgroundPaint(Color.white);

    super.setChart(chart);

    // reset zoom history
    ZoomHistory history = getZoomHistory();
    if (history != null)
      history.clear();
  }

  void updateData(List<RetTimeClusterer.Cluster> clusters) {
    xyDataset.setNotify(false);
    xyDataset.removeAllSeries();

    int seriesID = 0;
    for (RetTimeClusterer.Cluster c : clusters) {
      XYSeries series = new XYSeries(seriesID++, false);
      for (BetterPeak peak : c.peaks) {
        series.add(peak.getFirstRetTime(), peak.getMZ());
        series.add(peak.getLastRetTime(), peak.getMZ());
      }
      xyDataset.addSeries(series);
    }
    xyDataset.setNotify(true);
  }

  public void setDomain(Range<Double> range) {
    plot.getDomainAxis().setRange(range.lowerEndpoint(), range.upperEndpoint());
  }
}
