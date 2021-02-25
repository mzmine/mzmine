/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 3.
 * 
 * MZmine 3 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 3 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 3; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.imageplot;

import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;
import java.util.Arrays;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYZDataset;
import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockRendererSmallBlocks;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleFactory;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class ImageHeatMapPlot extends EChartViewer {

  private final XYPlot plot;
  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 8);
  private PaintScaleLegend legend;
  private XYBlockRendererSmallBlocks blockRenderer;
  private double dataPointHeight;
  private double dataPointWidth;
  private PaintScale paintScale;

  public ImageHeatMapPlot(XYZDataset dataset, PaintScale paintScale, double dataPointWidth,
      double dataPointHeight) {

    super(ChartFactory.createScatterPlot("", "[\u00B5m]", "[\u00B5m]", dataset,
        PlotOrientation.VERTICAL, true, true, true));
    this.dataPointWidth = dataPointWidth;
    this.dataPointHeight = dataPointHeight;
    this.paintScale = paintScale;
    JFreeChart chart = getChart();
    // copy and sort z-Values for min and max of the paint scale and axis
    double[] copyXValues = new double[dataset.getItemCount(0)];
    double[] copyYValues = new double[dataset.getItemCount(0)];
    double[] copyZValues = new double[dataset.getItemCount(0)];
    for (int i = 0; i < dataset.getItemCount(0); i++) {
      copyXValues[i] = dataset.getXValue(0, i);
      copyYValues[i] = dataset.getYValue(0, i);
      copyZValues[i] = dataset.getZValue(0, i);
    }
    Arrays.sort(copyXValues);
    Arrays.sort(copyYValues);
    Arrays.sort(copyZValues);
    double min = copyZValues[0];
    double max = copyZValues[copyZValues.length - 1];

    updatePaintScale(paintScale);
    PaintScaleFactory paintScaleFactoy = new PaintScaleFactory();
    paintScaleFactoy.createColorsForPaintScale(paintScale);

    plot = chart.getXYPlot();
    EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(chart);
    ((NumberAxis) chart.getXYPlot().getRangeAxis())
        .setNumberFormatOverride(new DecimalFormat("0.0E0"));
    ((NumberAxis) chart.getXYPlot().getRangeAxis()).setRange(0,
        copyYValues[copyYValues.length - 1]);
    ((NumberAxis) chart.getXYPlot().getDomainAxis())
        .setNumberFormatOverride(new DecimalFormat("0.0E0"));
    ((NumberAxis) chart.getXYPlot().getDomainAxis()).setRange(0,
        copyXValues[copyXValues.length - 1]);

    setPixelRenderer();
    prepareLegend(min, max);

    blockRenderer.setPaintScale(paintScale);
    plot.setRenderer(blockRenderer);
    plot.setBackgroundPaint(Color.black);
    plot.setRangeGridlinePaint(Color.black);
    plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
    plot.setOutlinePaint(Color.black);
    chart.addSubtitle(legend);
  }

  private void setPixelRenderer() {
    blockRenderer = new XYBlockRendererSmallBlocks();
    blockRenderer.setBlockHeight(dataPointHeight);
    blockRenderer.setBlockWidth(dataPointWidth);
  }

  private void prepareLegend(double min, double max) {
    NumberAxis scaleAxis = new NumberAxis(null);
    scaleAxis.setNumberFormatOverride(new DecimalFormat("0.0E0"));
    scaleAxis.setRange(min, max);
    scaleAxis.setAxisLinePaint(Color.white);
    scaleAxis.setTickMarkPaint(Color.white);
    legend = new PaintScaleLegend(paintScale, scaleAxis);
    legend.setPadding(5, 0, 5, 0);
    legend.setStripOutlineVisible(false);
    legend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    legend.setAxisOffset(5.0);
    legend.setSubdivisionCount(500);
    legend.setPosition(RectangleEdge.RIGHT);
    legend.getAxis().setLabelFont(legendFont);
    legend.getAxis().setTickLabelFont(legendFont);
  }

  public XYPlot getPlot() {
    return plot;
  }

  public double getDataPointHeight() {
    return dataPointHeight;
  }

  public void setDataPointHeight(double dataPointHeight) {
    this.dataPointHeight = dataPointHeight;
  }

  public double getDataPointWidth() {
    return dataPointWidth;
  }

  public void setDataPointWidth(double dataPointWidth) {
    this.dataPointWidth = dataPointWidth;
  }

  public PaintScale getPaintScale() {
    return paintScale;
  }

  public void setPaintScale(PaintScale paintScale) {
    this.paintScale = paintScale;
  }

  public void updatePaintScale(PaintScale paintScaleParameters) {
    PaintScale newPaintScale = new PaintScale(paintScaleParameters.getPaintScaleColorStyle(),
        paintScaleParameters.getPaintScaleBoundStyle(),
        Range.closed(paintScale.getLowerBound(), paintScale.getUpperBound()));
    PaintScaleFactory paintScaleFactoy = new PaintScaleFactory();
    paintScaleFactoy.createColorsForPaintScale(newPaintScale);
    paintScale = newPaintScale;
  }
}
