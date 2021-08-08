/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package io.github.mzmine.modules.dataprocessing.adap_hierarchicalclustering;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.data.xy.DefaultXYZDataset;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import javafx.scene.Cursor;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */

public class SimpleScatterPlot extends EChartViewer {

  private static final int SERIES_ID = 0;

  private final JFreeChart chart;
  private final XYPlot plot;
  private final NumberAxis xAxis, yAxis;
  private final DefaultXYZDataset xyDataset;

  public SimpleScatterPlot(String xLabel, String yLabel) {
    this(new double[0], new double[0], new double[0], xLabel, yLabel);
  }

  public SimpleScatterPlot(double[] xValues, double[] yValues, double[] colors, String xLabel,
      String yLabel) {
    super(null);

    // setBackground(Color.white);
    setCursor(Cursor.CROSSHAIR);

    xAxis = new NumberAxis(xLabel);
    xAxis.setAutoRangeIncludesZero(false);
    xAxis.setUpperMargin(0);
    xAxis.setLowerMargin(0);

    yAxis = new NumberAxis(yLabel);
    yAxis.setAutoRangeIncludesZero(false);
    yAxis.setUpperMargin(0);
    yAxis.setLowerMargin(0);

    xyDataset = new DefaultXYZDataset();
    int length = Math.min(xValues.length, yValues.length);
    double[][] data = new double[3][length];
    System.arraycopy(xValues, 0, data[0], 0, length);
    System.arraycopy(yValues, 0, data[1], 0, length);
    System.arraycopy(colors, 0, data[2], 0, length);
    xyDataset.addSeries(SERIES_ID, data);

    XYDotRenderer renderer = new XYDotRenderer() {
      @Override
      public Paint getItemPaint(int row, int col) {
        double c = xyDataset.getZ(row, col).doubleValue();
        return Color.getHSBColor((float) c, 1.0f, 1.0f);
      }
    };

    renderer.setDotHeight(3);
    renderer.setDotWidth(3);

    plot = new XYPlot(xyDataset, xAxis, yAxis, renderer);
    plot.setBackgroundPaint(Color.white);
    plot.setDomainGridlinesVisible(true);
    plot.setRangeGridlinesVisible(true);

    chart = new JFreeChart("", new Font("SansSerif", Font.BOLD, 12), plot, false);
    chart.setBackgroundPaint(Color.white);

    super.setChart(chart);

    // reset zoom history
    ZoomHistory history = getZoomHistory();
    if (history != null)
      history.clear();
  }

  public void updateData(double[] xValues, double[] yValues, double[] colors) {
    xyDataset.removeSeries(SERIES_ID);

    int length = Math.min(xValues.length, yValues.length);

    double[][] data = new double[3][length];
    System.arraycopy(xValues, 0, data[0], 0, length);
    System.arraycopy(yValues, 0, data[1], 0, length);
    System.arraycopy(colors, 0, data[2], 0, length);
    xyDataset.addSeries(SERIES_ID, data);
  }
}
