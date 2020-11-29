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

package io.github.mzmine.modules.visualization.msms;

import io.github.mzmine.datamodel.features.FeatureList;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseWheelEvent;
import java.text.NumberFormat;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleInsets;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import io.github.mzmine.main.MZmineCore;

/**
 *
 */
class MsMsPlot extends EChartViewer  {

  private RawDataFile rawDataFile;
  private Range<Double> mzRange;
  private Range<Float> rtRange;

  private JFreeChart chart;

  private XYPlot plot;

  // Zoom factor.
  private static final double ZOOM_FACTOR = 1.2;

  // VisualizerWindow visualizer.
  private final MsMsVisualizerTab visualizer;

  private FeatureDataRenderer featureDataRenderer;

  // grid color
  private static final Color gridColor = Color.lightGray;

  // Renderers
  private MsMsPlotRenderer mainRenderer;

  // crosshair (selection) color
  private static final Color crossHairColor = Color.gray;

  // crosshair stroke
  private static final BasicStroke crossHairStroke =
      new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[] {5, 3}, 0);

  // title font
  private static final Font titleFont = new Font("SansSerif", Font.BOLD, 12);
  private static final Font subTitleFont = new Font("SansSerif", Font.PLAIN, 11);
  private TextTitle chartTitle, chartSubTitle;

  private NumberAxis xAxis, yAxis;

  private NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
  private NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

  MsMsPlot(RawDataFile rawDataFile, MsMsVisualizerTab visualizer, MsMsDataSet dataset,
      Range<Float> rtRange, Range<Double> mzRange) {

    super(ChartFactory.createXYLineChart("", "", "", null, PlotOrientation.VERTICAL, true, true,
        false), true, true, false, false, true);

    this.visualizer = visualizer;
    this.rawDataFile = rawDataFile;
    this.rtRange = rtRange;
    this.mzRange = mzRange;

    // initialize the chart by default time series chart from factory
    chart = getChart();
    chart.setBackgroundPaint(Color.white);

    // disable maximum size (we don't want scaling)
    // setMaximumDrawWidth(Integer.MAX_VALUE);
    // setMaximumDrawHeight(Integer.MAX_VALUE);

    // set the plot properties
    plot = chart.getXYPlot();
    plot.setBackgroundPaint(Color.white);
    plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
    plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
    plot.setDomainGridlinePaint(gridColor);
    plot.setRangeGridlinePaint(gridColor);

    // Set the domain log axis
    xAxis = new NumberAxis("Retention time (min)");
    xAxis.setAutoRangeIncludesZero(false);
    xAxis.setNumberFormatOverride(rtFormat);
    xAxis.setUpperMargin(0.01);
    xAxis.setLowerMargin(0.01);
    plot.setDomainAxis(xAxis);

    // Set the range log axis
    yAxis = new NumberAxis("Precursor m/z");
    yAxis.setAutoRangeIncludesZero(false);
    yAxis.setNumberFormatOverride(mzFormat);
    yAxis.setUpperMargin(0.1);
    yAxis.setLowerMargin(0.1);
    plot.setRangeAxis(yAxis);

    // Set crosshair properties
    plot.setDomainCrosshairVisible(true);
    plot.setRangeCrosshairVisible(true);
    plot.setDomainCrosshairPaint(crossHairColor);
    plot.setRangeCrosshairPaint(crossHairColor);
    plot.setDomainCrosshairStroke(crossHairStroke);
    plot.setRangeCrosshairStroke(crossHairStroke);

    // Create renderers
    mainRenderer = new MsMsPlotRenderer();
    plot.setRenderer(0, mainRenderer);

    // title
    chartTitle = chart.getTitle();
    chartTitle.setMargin(5, 0, 0, 0);
    chartTitle.setFont(titleFont);
    chartSubTitle = new TextTitle();
    chartSubTitle.setFont(subTitleFont);
    chartSubTitle.setMargin(5, 0, 0, 0);
    chart.addSubtitle(chartSubTitle);

    // Add data sets;
    plot.setDataset(0, dataset);

    // set rendering order
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

    featureDataRenderer = new FeatureDataRenderer();


    // reset zoom history
    ZoomHistory history = getZoomHistory();
    if (history != null)
      history.clear();
  }

  XYPlot getXYPlot() {
    return plot;
  }

  void setTitle(String title) {
    chartTitle.setText(title);
  }

  void loadFeatureList(FeatureList featureList) {

    FeatureDataSet featureDataSet = new FeatureDataSet(rawDataFile, featureList, rtRange, mzRange);

    plot.setDataset(1, featureDataSet);
    plot.setRenderer(1, featureDataRenderer);
  }

  void switchDataPointsVisible() {
    boolean dataPointsVisible = featureDataRenderer.getDefaultShapesVisible();
    featureDataRenderer.setDefaultShapesVisible(!dataPointsVisible);
  }

  public void showFeaturesTooltips(boolean mode) {
    if (mode) {
      FeatureToolTipGenerator toolTipGenerator = new FeatureToolTipGenerator();
      this.featureDataRenderer.setDefaultToolTipGenerator(toolTipGenerator);
    } else {
      this.featureDataRenderer.setDefaultToolTipGenerator(null);
    }
  }


  public void mouseWheelMoved(MouseWheelEvent event) {
    int notches = event.getWheelRotation();
    if (notches < 0) {
      getXYPlot().getDomainAxis().resizeRange(1.0 / ZOOM_FACTOR);
    } else {
      getXYPlot().getDomainAxis().resizeRange(ZOOM_FACTOR);
    }
  }

  public void addMsMsDataSet(MsMsDataSet dataset) {
    plot.setDataset(dataset);
    plot.setRenderer(mainRenderer);
  }
}
