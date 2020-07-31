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

package io.github.mzmine.modules.dataprocessing.masscalibration.charts;


import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.modules.dataprocessing.masscalibration.MassCalibrator;
import io.github.mzmine.modules.dataprocessing.masscalibration.MassPeakMatch;
import io.github.mzmine.modules.dataprocessing.masscalibration.errormodeling.DistributionRange;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.function.Function2D;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.List;
import java.util.Map;

/**
 * Chart for error size vs mz ratio plots (xy scatter plot of error values vs mz ratio)
 * with additional extraction ranges and bias estimates lines shown up
 */
public class ErrorVsMzChart extends EChartViewer {

  private final JFreeChart chart;
  private final XYPlot plot;

  protected ErrorVsMzChart(JFreeChart chart) {
    super(chart);
    this.chart = chart;
    this.plot = chart.getXYPlot();
  }

  public ErrorVsMzChart(String title) {
    this(createEmptyChart(title));
  }

  public ErrorVsMzChart() {
    this("Error size vs mz ratio");
  }

  public static JFreeChart createEmptyChart(String title) {
    NumberAxis xAxis = new NumberAxis("Measured m/z ratio");
    NumberAxis yAxis = new NumberAxis("PPM error");
    XYPlot plot = new XYPlot(null, xAxis, yAxis, null);
    plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);

    plot.setRenderer(0, ChartUtils.createErrorsRenderer());
    plot.setRenderer(1, ChartUtils.createTrendRenderer());
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

    return new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
  }

  public void cleanPlot() {
    ChartUtils.cleanPlot(plot);
  }

  public void updatePlot(List<MassPeakMatch> matches, Map<String, DistributionRange> errorRanges,
                         double biasEstimate, Trend2D errorVsMzTrend) {
    updateChartDataset(matches, errorVsMzTrend);

    for (String label : errorRanges.keySet()) {
      DistributionRange errorRange = errorRanges.get(label);
      Range<Double> errorValueRange = errorRange.getValueRange();

      ValueMarker valueMarkerLower = ChartUtils.createValueMarker(label + " lower", errorValueRange.lowerEndpoint());
      ValueMarker valueMarkerUpper = ChartUtils.createValueMarker(label + " upper", errorValueRange.upperEndpoint());
      plot.addRangeMarker(valueMarkerLower);
      plot.addRangeMarker(valueMarkerUpper);
    }
    plot.addRangeMarker(ChartUtils.createValueMarker("Bias estimate", biasEstimate));
  }

  protected void updateChartDataset(List<MassPeakMatch> matches) {
    XYSeries errorsXY = new XYSeries("PPM errors");
    for (MassPeakMatch match : matches) {
      double error = MassCalibrator.massError.calculateError(match.getMeasuredMzRatio(), match.getMatchedMzRatio());
      errorsXY.add(match.getMeasuredMzRatio(), error);
    }

    XYSeriesCollection dataset = new XYSeriesCollection(errorsXY);
    plot.setDataset(0, dataset);

    Function2D trend = new WeightedKnnTrend(errorsXY);
    XYSeries trendSeries = DatasetUtils.sampleFunction2DToSeries(trend, dataset.getDomainLowerBound(false),
            dataset.getDomainUpperBound(false), 1000, "trend series");
    XYSeriesCollection trendDataset = new XYSeriesCollection(trendSeries);
    plot.setDataset(1, trendDataset);
  }

  protected void updateChartDataset(List<MassPeakMatch> matches, Trend2D trend) {
    XYSeries errorsXY = new XYSeries("PPM errors");
    for (MassPeakMatch match : matches) {
      double error = MassCalibrator.massError.calculateError(match.getMeasuredMzRatio(), match.getMatchedMzRatio());
      errorsXY.add(match.getMeasuredMzRatio(), error);
    }

    XYSeriesCollection dataset = new XYSeriesCollection(errorsXY);
    plot.setDataset(0, dataset);

    if (trend != null) {
      XYSeries trendSeries = DatasetUtils.sampleFunction2DToSeries(trend, dataset.getDomainLowerBound(false),
              dataset.getDomainUpperBound(false), 1000, "trend series");
      XYSeriesCollection trendDataset = new XYSeriesCollection(trendSeries);
      plot.setDataset(1, trendDataset);
    }
  }
}
