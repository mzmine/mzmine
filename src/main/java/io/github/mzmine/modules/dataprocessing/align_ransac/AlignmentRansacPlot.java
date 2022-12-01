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
package io.github.mzmine.modules.dataprocessing.align_ransac;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.fitting.PolynomialFitter;
import org.apache.commons.math.optimization.general.GaussNewtonOptimizer;
import org.apache.commons.math.stat.regression.SimpleRegression;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import io.github.mzmine.main.MZmineCore;

public class AlignmentRansacPlot extends EChartViewer {

  // peak labels color
  private static final Color labelsColor = Color.darkGray;
  // grid color
  private static final Color gridColor = Color.lightGray;
  // crosshair (selection) color
  private static final Color crossHairColor = Color.gray;
  // crosshair stroke
  private static final BasicStroke crossHairStroke =
      new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[] {5, 3}, 0);
  // data points shape
  private static final Shape dataPointsShape = new Ellipse2D.Double(-2, -2, 5, 5);
  // titles
  private static final Font titleFont = new Font("SansSerif", Font.BOLD, 12);
  private TextTitle chartTitle;
  // legend
  private LegendTitle legend;
  private static final Font legendFont = new Font("SansSerif", Font.PLAIN, 11);
  private XYToolTipGenerator toolTipGenerator;
  private XYSeriesCollection dataset;
  private JFreeChart chart;
  private XYPlot plot;
  private NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();

  public AlignmentRansacPlot() {
    super(ChartFactory.createXYLineChart("", null, null, new XYSeriesCollection(),
        PlotOrientation.VERTICAL, true, true, false));

    chart = this.getChart();
    chart.setBackgroundPaint(Color.white);

    // title
    chartTitle = chart.getTitle();
    chartTitle.setMargin(5, 0, 0, 0);
    chartTitle.setFont(titleFont);

    // legend constructed by ChartFactory
    legend = chart.getLegend();
    legend.setItemFont(legendFont);
    legend.setFrame(BlockBorder.NONE);

    // set the plot properties
    plot = chart.getXYPlot();
    plot.setBackgroundPaint(Color.white);
    plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
    dataset = (XYSeriesCollection) plot.getDataset();

    // set grid properties
    plot.setDomainGridlinePaint(gridColor);
    plot.setRangeGridlinePaint(gridColor);

    // set crosshair (selection) properties

    plot.setDomainCrosshairVisible(true);
    plot.setRangeCrosshairVisible(true);
    plot.setDomainCrosshairPaint(crossHairColor);
    plot.setRangeCrosshairPaint(crossHairColor);
    plot.setDomainCrosshairStroke(crossHairStroke);
    plot.setRangeCrosshairStroke(crossHairStroke);

    // set default renderer properties
    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
    renderer.setDefaultLinesVisible(false);
    renderer.setDefaultShapesVisible(true);
    renderer.setSeriesShape(0, dataPointsShape);
    renderer.setSeriesShape(1, dataPointsShape);
    renderer.setSeriesLinesVisible(2, true);
    renderer.setSeriesShapesVisible(2, false);
    renderer.setSeriesPaint(0, Color.RED);
    renderer.setSeriesPaint(1, Color.GRAY);
    renderer.setSeriesPaint(2, Color.BLUE);
    renderer.setDefaultItemLabelPaint(labelsColor);
    renderer.setDrawSeriesLineAsPath(true);

    plot.setRenderer(renderer);

    // reset zoom history
    ZoomHistory history = getZoomHistory();
    if (history != null)
      history.clear();
  }

  /**
   * Remove all series from the chart
   */
  public void removeSeries() {
    dataset.removeAllSeries();
  }

  /**
   * Add new series.
   *
   * @param data Vector with the alignments
   * @param title Name of the feature lists in this alignment
   */
  public void addSeries(Vector<AlignStructMol> data, String title, boolean linear) {
    try {
      chart.setTitle(title);
      XYSeries s1 = new XYSeries("Aligned pairs");
      XYSeries s2 = new XYSeries("Non-aligned pairs");
      XYSeries s3 = new XYSeries("Model");

      PolynomialFunction function = getPolynomialFunction(data, linear);

      for (AlignStructMol point : data) {

        if (point.Aligned) {
          s1.add(point.row1.getFeatures().get(0).getRT(), point.row2.getFeatures().get(0).getRT());
        } else {
          s2.add(point.row1.getFeatures().get(0).getRT(), point.row2.getFeatures().get(0).getRT());
        }
        try {
          s3.add(function.value(point.row2.getFeatures().get(0).getRT()),
              point.row2.getFeatures().get(0).getRT());
        } catch (Exception e) {
        }
      }

      this.dataset.addSeries(s1);
      this.dataset.addSeries(s2);
      this.dataset.addSeries(s3);

    } catch (Exception e) {
    }
  }

  private PolynomialFunction getPolynomialFunction(Vector<AlignStructMol> list, boolean linear) {
    List<RTs> data = new ArrayList<RTs>();
    for (AlignStructMol m : list) {
      if (m.Aligned) {
        data.add(new RTs(m.RT2, m.RT));
      }
    }

    data = this.smooth(data);
    Collections.sort(data, new RTs());

    double[] xval = new double[data.size()];
    double[] yval = new double[data.size()];
    int i = 0;

    for (RTs rt : data) {
      xval[i] = rt.RT;
      yval[i++] = rt.RT2;
    }

    int degree = 2;
    if (linear) {
      degree = 1;
    }

    PolynomialFitter fitter = new PolynomialFitter(degree, new GaussNewtonOptimizer(true));
    for (RTs rt : data) {
      fitter.addObservedPoint(1, rt.RT, rt.RT2);
    }
    try {
      return fitter.fit();

    } catch (Exception ex) {
      return null;
    }
  }

  private List<RTs> smooth(List<RTs> list) {
    // Add points to the model in between of the real points to smooth the
    // regression model
    Collections.sort(list, new RTs());

    for (int i = 0; i < list.size() - 1; i++) {
      RTs point1 = list.get(i);
      RTs point2 = list.get(i + 1);
      if (point1.RT < point2.RT - 2) {
        SimpleRegression regression = new SimpleRegression();
        regression.addData(point1.RT, point1.RT2);
        regression.addData(point2.RT, point2.RT2);
        double rt = point1.RT + 1;
        while (rt < point2.RT) {
          RTs newPoint = new RTs(rt, regression.predict(rt));
          list.add(newPoint);
          rt++;
        }

      }
    }

    return list;
  }

  public void printAlignmentChart(String axisTitleX, String axisTitleY) {
    try {
      toolTipGenerator = new AlignmentPreviewTooltipGenerator(axisTitleX, axisTitleY);
      plot.getRenderer().setDefaultToolTipGenerator(toolTipGenerator);
      NumberAxis xAxis = new NumberAxis(axisTitleX);
      xAxis.setNumberFormatOverride(rtFormat);
      xAxis.setAutoRangeIncludesZero(false);
      plot.setDomainAxis(xAxis);

      NumberAxis yAxis = new NumberAxis(axisTitleY);
      yAxis.setNumberFormatOverride(rtFormat);
      yAxis.setAutoRangeIncludesZero(false);
      plot.setRangeAxis(yAxis);

    } catch (Exception e) {
    }
  }
}
