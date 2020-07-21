/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.masscalibration;


import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.modules.dataprocessing.masscalibration.errormodeling.DistributionRange;
import javafx.scene.shape.Circle;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.function.Function2D;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.Map;

/**
 * Chart for error size vs mz ratio plots (xy scatter plot of error values vs mz ratio)
 * with additional extraction ranges and bias estimates lines shown up
 */
public class ErrorVsMzChart extends EChartViewer {

  private final JFreeChart chart;

  protected ErrorVsMzChart(JFreeChart chart) {
    super(chart);
    this.chart = chart;
  }

  public ErrorVsMzChart(String title) {
    this(createEmptyChart(title));
  }

  public ErrorVsMzChart() {
    this("Error size vs mz ratio");
  }

  public static JFreeChart createEmptyChart(String title) {
//    XYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES);
    NumberAxis xAxis = new NumberAxis("Measured m/z ratio");
    NumberAxis yAxis = new NumberAxis("PPM error");
//    XYPlot plot = new XYPlot(null, xAxis, yAxis, renderer);
    XYPlot plot = new XYPlot(null, xAxis, yAxis, null);
    plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);


    /*XYLineAndShapeRenderer errorsRenderer = new XYLineAndShapeRenderer(false, true);
//    Shape circle = new Ellipse2D.Double(-3, -3, 6, 6);
    Shape circle = new Ellipse2D.Double(-2, -2, 4, 4);
    errorsRenderer.setDefaultShape(circle);
    errorsRenderer.setSeriesShape(0, circle);
    Color paintColor = new Color(230, 160, 30);
    errorsRenderer.setDefaultPaint(paintColor);
    errorsRenderer.setSeriesPaint(0, paintColor);
    errorsRenderer.setDefaultFillPaint(paintColor);
    errorsRenderer.setSeriesFillPaint(0, paintColor);
    errorsRenderer.setUseFillPaint(true);
    errorsRenderer.setUseOutlinePaint(true);
    errorsRenderer.setDefaultOutlinePaint(paintColor);
    errorsRenderer.setSeriesOutlinePaint(0, paintColor);*/
    plot.setRenderer(0, ChartUtils.createErrorsRenderer());

    /*XYLineAndShapeRenderer trendRenderer = new XYLineAndShapeRenderer();
//    Shape smallCircle = new Ellipse2D.Double(0, 0, 2, 2);
//    trendRenderer.setDefaultShape(smallCircle);
//    trendRenderer.setSeriesShape(0, smallCircle);
    trendRenderer.setDefaultShape(circle);
    trendRenderer.setSeriesShape(0, circle);
//    trendRenderer.setDefaultStroke(new BasicStroke(5.0f));
    trendRenderer.setDefaultStroke(new BasicStroke(5));
//    trendRenderer.setSeriesStroke(0, new BasicStroke(2.0f));
//    trendRenderer.setSeriesStroke(0, new BasicStroke(5));
    trendRenderer.setSeriesStroke(0, new BasicStroke(2));
    trendRenderer.setAutoPopulateSeriesStroke(false);
//    renderer.setBaseStroke(new BasicStroke(2.0f));
//    renderer.setAutoPopulateSeriesStroke(false);
    plot.setRenderer(1, trendRenderer);*/

    plot.setRenderer(1, ChartUtils.createTrendRenderer());

    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);


    return new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
  }

  public void cleanPlot() {
    XYPlot plot = chart.getXYPlot();
    for (int i = 0; i < plot.getDatasetCount(); i++) {
      plot.setDataset(i, null);
    }
    plot.clearRangeMarkers();
  }

  public void updatePlot(List<MassPeakMatch> matches, Map<String, DistributionRange> errorRanges,
                         double biasEstimate) {
    XYPlot plot = chart.getXYPlot();

//    XYDataset dataset = createChartDataset(matches);
//    plot.setDataset(dataset);
//    plot.setDataset(0, dataset);
    updateChartDataset(matches);

    for (String label : errorRanges.keySet()) {
      DistributionRange errorRange = errorRanges.get(label);
      Range<Double> errorValueRange = errorRange.getValueRange();

      ValueMarker valueMarkerLower = createValueMarker(label + " lower", errorValueRange.lowerEndpoint());
      ValueMarker valueMarkerUpper = createValueMarker(label + " upper", errorValueRange.upperEndpoint());
      plot.addRangeMarker(valueMarkerLower);
      plot.addRangeMarker(valueMarkerUpper);
    }
    plot.addRangeMarker(createValueMarker("Bias estimate", biasEstimate));
  }

//  protected XYDataset createChartDataset(List<MassPeakMatch> matches) {
  protected void updateChartDataset(List<MassPeakMatch> matches) {
    XYPlot plot = chart.getXYPlot();

    XYSeries errorsXY = new XYSeries("PPM errors");
    for (MassPeakMatch match : matches) {
      double error = MassCalibrator.massError.calculateError(match.getMeasuredMzRatio(), match.getMatchedMzRatio());
      errorsXY.add(match.getMeasuredMzRatio(), error);
    }

    XYSeriesCollection dataset = new XYSeriesCollection(errorsXY);
    plot.setDataset(0, dataset);



//    XYSeriesCollection trendDataset = new XYSeriesCollection();
    Function2D trend = new WeightedKnnTrend(errorsXY);
    XYSeries trendSeries = DatasetUtils.sampleFunction2DToSeries(trend, dataset.getDomainLowerBound(false),
            dataset.getDomainUpperBound(false), 1000, "trend series");
//    dataset.addSeries(trendSeries);
    XYSeriesCollection trendDataset = new XYSeriesCollection(trendSeries);
    plot.setDataset(1, trendDataset);

//    XYItemRenderer trendRendered = plot.getRenderer(1);
//    XYLineAndShapeRenderer trendRendered = (XYLineAndShapeRenderer) plot.getRenderer(1);
//    trendRendered.setDefaultLinesVisible(true);

//    XYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES);
    /*XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
    renderer.setDefaultShape(new Ellipse2D.Double(0, 0, 10, 10));
    renderer.setSeriesShape(0, new Ellipse2D.Double(0, 0, 10, 10));*/
//    renderer.setDefaultPaint();
//    XYLineAndShapeRenderer trendRendered = new XYLineAndShapeRenderer();
//    plot.setRenderer(0, renderer);
//    plot.setRenderer(1, trendRendered);

//    return dataset;
  }

  protected ValueMarker createValueMarker(String label, double value) {
    ValueMarker valueMarker = new ValueMarker(value);
    valueMarker.setLabel(String.format("%s: %.4f", label, value));
    valueMarker.setPaint(Color.blue);
    valueMarker.setLabelTextAnchor(TextAnchor.BASELINE_LEFT);
    valueMarker.setLabelPaint(Color.blue);
//    valueMarker.setLabelFont(new Font(null, Font.BOLD, 11));
    valueMarker.setLabelFont(new Font(null, 0, 11));
    return valueMarker;
  }

}
