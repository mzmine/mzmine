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
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Chart for error distribution (xy scatter plot of error values vs match number, sorted by error size)
 * with additional extraction ranges and bias estimates lines shown up
 */
public class ErrorDistributionChart extends EChartViewer {

  private final JFreeChart distributionChart;

  protected ErrorDistributionChart(JFreeChart chart) {
    super(chart);
    distributionChart = chart;
  }

  public ErrorDistributionChart(String title) {
    this(createEmptyDistributionChart(title));
  }

  public ErrorDistributionChart() {
    this("Error distribution");
  }

  public static JFreeChart createEmptyDistributionChart(String title) {
    XYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.SHAPES);
    NumberAxis xAxis = new NumberAxis("Match number");
    NumberAxis yAxis = new NumberAxis("PPM error");
    XYPlot plot = new XYPlot(null, xAxis, yAxis, renderer);
    plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);

    return new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
  }

  public void cleanDistributionPlot() {
    XYPlot distributionPlot = distributionChart.getXYPlot();
    for (int i = 0; i < distributionPlot.getDatasetCount(); i++) {
      distributionPlot.setDataset(i, null);
    }
    distributionPlot.clearRangeMarkers();
  }

  public void updateDistributionPlot(List<Double> errors, Map<String, DistributionRange> errorRanges,
                                     double biasEstimate) {
    XYPlot distributionPlot = distributionChart.getXYPlot();

    XYDataset dataset = createDistributionDataset(errors);
    distributionPlot.setDataset(dataset);

    for (String label : errorRanges.keySet()) {
      DistributionRange errorRange = errorRanges.get(label);
      Range<Double> errorValueRange = errorRange.getValueRange();

      ValueMarker valueMarkerLower = createValueMarker(label + " lower", errorValueRange.lowerEndpoint());
      ValueMarker valueMarkerUpper = createValueMarker(label + " upper", errorValueRange.upperEndpoint());
      distributionPlot.addRangeMarker(valueMarkerLower);
      distributionPlot.addRangeMarker(valueMarkerUpper);
    }
    distributionPlot.addRangeMarker(createValueMarker("Bias estimate", biasEstimate));
  }

  protected XYDataset createDistributionDataset(List<Double> errors) {
    XYSeries errorsXY = new XYSeries("PPM errors");
    for (int i = 0; i < errors.size(); i++) {
      errorsXY.add(i + 1, errors.get(i));
    }

    return new XYSeriesCollection(errorsXY);
  }

  protected ValueMarker createValueMarker(String label, double value) {
    ValueMarker valueMarker = new ValueMarker(value);
    valueMarker.setLabel(String.format("%s: %.4f", label, value));
    valueMarker.setPaint(Color.blue);
    valueMarker.setLabelTextAnchor(TextAnchor.BASELINE_LEFT);
    valueMarker.setLabelPaint(Color.blue);
    return valueMarker;
  }

}
