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
 * Chart for measured mz vs matched mz plots (xy scatter plot of measured mz vs matched mz)
 * with additional extraction ranges and bias estimates lines shown up
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

    return new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
  }

  public void cleanPlot() {
    XYPlot distributionPlot = chart.getXYPlot();
    for (int i = 0; i < distributionPlot.getDatasetCount(); i++) {
      distributionPlot.setDataset(i, null);
    }
    distributionPlot.clearRangeMarkers();
  }

  public void updatePlot(List<MassPeakMatch> matches) {
    XYPlot distributionPlot = chart.getXYPlot();

    XYDataset dataset = createChartDataset(matches);
    distributionPlot.setDataset(dataset);
  }

  protected XYDataset createChartDataset(List<MassPeakMatch> matches) {
    XYSeries errorsXY = new XYSeries("Mz matches");
    for (MassPeakMatch match : matches) {
      errorsXY.add(match.getMatchedMzRatio(), match.getMeasuredMzRatio());
    }

    return new XYSeriesCollection(errorsXY);
  }

  protected ValueMarker createValueMarker(String label, double value) {
    ValueMarker valueMarker = new ValueMarker(value);
    valueMarker.setLabel(label);
    valueMarker.setPaint(Color.black);
    valueMarker.setLabelTextAnchor(TextAnchor.BASELINE_LEFT);
    return valueMarker;
  }

}
