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
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.ui.RectangleInsets;

public class CombinedModulePlot extends EChartViewer {

  private JFreeChart chart;

  private XYPlot plot;
  private RawDataFile dataFile;
  private CombinedModuleVisualizerTabController visualizer;
  private CombinedModuleDataset dataset;
  private Range<Float> rtRange;
  private Range<Double> mzRange;
  private Double noiseLevel;
  private ColorScale colorScale;
  private static final Color gridColor = Color.lightGray;
  // crosshair (selection) color
  private static final Color crossHairColor = Color.gray;

  // crosshair stroke
  private static final BasicStroke crossHairStroke =
      new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[]{5, 3},
          0);
  private Range<Double> highlightedPrecursorRange = Range.singleton(Double.NEGATIVE_INFINITY);
  private Range<Double> highlightedNeutralLossRange = Range.singleton(Double.NEGATIVE_INFINITY);


  private NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
  private NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private CombinedModuleDataPointRenderer defaultRenderer;


  public CombinedModulePlot() {
    super(ChartFactory.createXYLineChart("", "", "", null, PlotOrientation.VERTICAL, true, true,
        false), true, true, false, false, true);
  }

  public void setPlot(RawDataFile dataFile, CombinedModuleVisualizerTabController visualizer,
      Range<Float> rtRange, Range<Double> mzRange, AxisType xAxisType, AxisType yAxisType,
      Double noiseLevel, ColorScale colorScale) {

    this.visualizer = visualizer;
    this.dataFile = dataFile;
    this.rtRange = rtRange;
    this.mzRange = mzRange;
    this.noiseLevel = noiseLevel;
    this.colorScale = colorScale;

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

    defaultRenderer = new CombinedModuleDataPointRenderer(false, true);
    defaultRenderer.setTransparency(0.4f);
    LookupPaintScale scale = new LookupPaintScale(rtRange.lowerEndpoint(), rtRange.upperEndpoint(),
        new Color(0, 0, 0));
    defaultRenderer.setPaintScale(scale);

    NumberAxis xAxis = (NumberAxis) this.plot.getDomainAxis();
    if (xAxisType == AxisType.RETENTIONTIME) {
      setAxes(xAxis, xAxisType, rtFormat);
    } else {
      setAxes(xAxis, xAxisType, mzFormat);
    }
    NumberAxis yAxis = (NumberAxis) this.plot.getRangeAxis();
    setAxes(yAxis, yAxisType, mzFormat);
  }

  private void setAxes(NumberAxis axis, AxisType axisType, NumberFormat format) {
    axis.setLabel(axisType.toString());
    axis.setAutoRangeIncludesZero(false);
    axis.setNumberFormatOverride(format);
    axis.setUpperMargin(0);
    axis.setLowerMargin(0);
  }

  XYPlot getXYPlot() {
    return plot;
  }

  public void addDataset(CombinedModuleDataset dataset) {
    plot.setDataset(dataset);
    defaultRenderer.setDefaultToolTipGenerator(dataset);
    plot.setRenderer(defaultRenderer);
  }

  /**
   * @return Returns the highlightedPrecursorRange.
   */
  Range<Double> getHighlightedPrecursorRange() {
    return highlightedPrecursorRange;
  }

  /**
   * @param range The highlightedPrecursorRange to set.
   */
  void setHighlightedPrecursorRange(Range<Double> range) {
    this.highlightedPrecursorRange = range;
  }

  /**
   * @return Returns the highlightedNeutralLossRange.
   */
  Range<Double> getHighlightedNeutralLossRange() {
    return highlightedNeutralLossRange;
  }

  /**
   * @param range The highlightedNeutralLossRange to set.
   */
  void setHighlightedNeutralLossRange(Range<Double> range) {
    this.highlightedNeutralLossRange = range;
  }
}
