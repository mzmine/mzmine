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
