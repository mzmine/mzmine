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

package io.github.mzmine.modules.visualization.combinedmodule;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import java.awt.BasicStroke;
import java.awt.Color;
import java.text.NumberFormat;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleInsets;

public class CombinedModulePlot extends EChartViewer {

  private JFreeChart chart;

  private XYPlot plot;
  private RawDataFile dataFile;
  private CombinedModuleVisualizerWindowController visualizer;
  private CombinedModuleDataset dataset;
  private Range<Double> rtRange;
  private Range<Double> mzRange;
  private String massList;
  private Double noiseLevel;
  private ColorScale colorScale;
  private NumberAxis xAxis, yAxis;
  private static final Color gridColor = Color.lightGray;
  // crosshair (selection) color
  private static final Color crossHairColor = Color.gray;

  // crosshair stroke
  private static final BasicStroke crossHairStroke =
      new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[]{5, 3}, 0);


  private NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
  private NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

  public CombinedModulePlot(){
    super(ChartFactory.createXYLineChart("", "", "", null, PlotOrientation.VERTICAL, true, true,
        false), true, true, false, false, true);


  }
  public CombinedModulePlot(RawDataFile dataFile,
      CombinedModuleVisualizerWindowController visualizer, CombinedModuleDataset dataset,
      Range<Double> rtRange, Range<Double> mzRange, AxisType xAxisType, AxisType yAxisType,
      String massList,
      Double noiseLevel, ColorScale colorScale) {
    super(ChartFactory.createXYLineChart("", "", "", null, PlotOrientation.VERTICAL, true, true,
        false), true, true, false, false, true);
    setMouseZoomable(false);

    this.visualizer = visualizer;
    this.dataFile = dataFile;
    this.rtRange = rtRange;
    this.mzRange = mzRange;

    chart = getChart();
    chart.setBackgroundPaint(Color.white);

    plot = chart.getXYPlot();
    plot.setBackgroundPaint(Color.white);
    plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
    plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
    plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
    plot.setDomainGridlinePaint(gridColor);
    plot.setRangeGridlinePaint(gridColor);
    plot.setDomainCrosshairVisible(true);
    plot.setRangeCrosshairVisible(true);
    plot.setDomainCrosshairPaint(crossHairColor);
    plot.setRangeCrosshairPaint(crossHairColor);
    plot.setDomainCrosshairStroke(crossHairStroke);
    plot.setRangeCrosshairStroke(crossHairStroke);

    setAxes(xAxis, xAxisType, rtFormat);
    plot.setDomainAxis(xAxis);
    setAxes(yAxis, yAxisType, mzFormat);
    plot.setRangeAxis(yAxis);

    plot.setDataset(0,dataset);

  }

  private void setAxes(NumberAxis axis, AxisType axisType, NumberFormat format) {
    axis = new NumberAxis(axisType.toString());
    axis.setAutoRangeIncludesZero(false);
    axis.setNumberFormatOverride(format);
    axis.setUpperMargin(0.01);
    axis.setLowerMargin(0.01);
  }
}
