/*
 * Copyright (C) 2017 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV2;


import java.awt.Color;
import java.awt.Cursor;
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
import dulab.adap.workflow.decomposition.RetTimeClusterer;
import net.sf.mzmine.chartbasics.EChartPanel;
import net.sf.mzmine.chartbasics.listener.ZoomHistory;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */


public class SimpleScatterPlot extends EChartPanel {
  private final XYPlot plot;
  private final XYSeriesCollection xyDataset;

  SimpleScatterPlot(String xLabel, String yLabel) {
    super(null, true);

    setBackground(Color.white);
    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

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
      for (RetTimeClusterer.Item range : c.ranges) {
        series.add(range.getInterval().lowerEndpoint().doubleValue(), range.getMZ());
        series.add(range.getInterval().upperEndpoint().doubleValue(), range.getMZ());
      }
      xyDataset.addSeries(series);
    }
    xyDataset.setNotify(true);
  }

  public void setDomain(Range<Double> range) {
    plot.getDomainAxis().setRange(range.lowerEndpoint(), range.upperEndpoint());
  }
}
