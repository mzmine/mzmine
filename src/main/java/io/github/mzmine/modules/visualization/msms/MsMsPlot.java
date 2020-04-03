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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
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
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import io.github.mzmine.main.MZmineCore;

/**
 *
 */
class MsMsPlot extends EChartViewer implements MouseWheelListener {

  private RawDataFile rawDataFile;
  private Range<Double> rtRange, mzRange;

  private JFreeChart chart;

  private XYPlot plot;

  // Zoom factor.
  private static final double ZOOM_FACTOR = 1.2;

  // VisualizerWindow visualizer.
  private final MsMsVisualizerWindow visualizer;

  private PeakDataRenderer peakDataRenderer;

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

  MsMsPlot(RawDataFile rawDataFile, MsMsVisualizerWindow visualizer, MsMsDataSet dataset,
      Range<Double> rtRange, Range<Double> mzRange) {

    super(null);

    this.visualizer = visualizer;
    this.rawDataFile = rawDataFile;
    this.rtRange = rtRange;
    this.mzRange = mzRange;

    // initialize the chart by default time series chart from factory
    chart = ChartFactory.createXYLineChart("", // title
        "", // x-axis label
        "", // y-axis label
        null, // data set
        PlotOrientation.VERTICAL, // orientation
        true, // create legend
        false, // generate tooltips
        false // generate URLs
    );

    chart.setBackgroundPaint(Color.white);
    setChart(chart);

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

    peakDataRenderer = new PeakDataRenderer();

    // Register for mouse-wheel events
    // addMouseWheelListener(this);

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

  void loadPeakList(PeakList peakList) {

    PeakDataSet peaksDataSet = new PeakDataSet(rawDataFile, peakList, rtRange, mzRange);

    plot.setDataset(1, peaksDataSet);
    plot.setRenderer(1, peakDataRenderer);
  }

  void switchDataPointsVisible() {
    boolean dataPointsVisible = peakDataRenderer.getDefaultShapesVisible();
    peakDataRenderer.setDefaultShapesVisible(!dataPointsVisible);
  }

  public void showPeaksTooltips(boolean mode) {
    if (mode) {
      PeakToolTipGenerator toolTipGenerator = new PeakToolTipGenerator();
      this.peakDataRenderer.setDefaultToolTipGenerator(toolTipGenerator);
    } else {
      this.peakDataRenderer.setDefaultToolTipGenerator(null);
    }
  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent event) {
    int notches = event.getWheelRotation();
    if (notches < 0) {
      getXYPlot().getDomainAxis().resizeRange(1.0 / ZOOM_FACTOR);
    } else {
      getXYPlot().getDomainAxis().resizeRange(ZOOM_FACTOR);
    }
  }

  /*
   * @Override public void mouseClicked(final MouseEvent event) {
   *
   * // Let the parent handle the event (selection etc.) super.mouseClicked(event);
   *
   * if (event.getX() < 70) { // User clicked on Y-axis if (event.getClickCount() == 2) { // Reset
   * zoom on Y-axis XYDataset data = ((XYPlot) getChart().getPlot()).getDataset(); Number maximum =
   * DatasetUtils.findMaximumRangeValue(data); getXYPlot().getRangeAxis().setRange(0, 1.05 *
   * maximum.floatValue()); } else if (event.getClickCount() == 1) { // Auto range on Y-axis
   * getXYPlot().getRangeAxis().setAutoTickUnitSelection(true);
   * getXYPlot().getRangeAxis().setAutoRange(true); } } else if (event.getY() >
   * this.getChartRenderingInfo().getPlotInfo().getPlotArea().getMaxY() - 41 &&
   * event.getClickCount() == 2) { // Reset zoom on X-axis
   * getXYPlot().getDomainAxis().setAutoTickUnitSelection(true); restoreAutoDomainBounds(); } else
   * if (event.getClickCount() == 2) { visualizer.actionPerformed( new
   * ActionEvent(event.getSource(), ActionEvent.ACTION_PERFORMED, "SHOW_SPECTRUM")); } }
   */
}
