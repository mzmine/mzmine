/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.ionmobilitytraceplot;

import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.chartutils.XYBlockRendererSmallBlocks;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScale;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleFactory;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import java.awt.Color;
import java.awt.Font;
import java.util.Arrays;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYZDataset;

public class RetentionTimeMobilityHeatMapPlot extends EChartViewer {

  private final XYPlot plot;
  static final Font legendFont = new Font("SansSerif", Font.PLAIN, 10);
  private PaintScaleLegend legend;
  private XYBlockRendererSmallBlocks blockRenderer;
  private double dataPointHeight;
  private double dataPointWidth;
  private final UnitFormat unitFormat = MZmineCore.getConfiguration().getUnitFormat();

  public RetentionTimeMobilityHeatMapPlot(XYZDataset dataset, PaintScale paintScale,
      double dataPointWidth, double dataPointHeight, @Nullable MobilityType mobilityType) {

    super(ChartFactory.createScatterPlot("", "Retention time", "Mobility", dataset,
        PlotOrientation.VERTICAL, true, true, true));

    this.dataPointWidth = dataPointWidth;
    this.dataPointHeight = dataPointHeight;

    JFreeChart chart = getChart();
    // copy and sort z-Values for min and max of the paint scale
    double[] copyZValues = new double[dataset.getItemCount(0)];
    for (int i = 0; i < dataset.getItemCount(0); i++) {
      copyZValues[i] = dataset.getZValue(0, i);
    }
    Arrays.sort(copyZValues);

    // get index in accordance to percentile windows
    int minIndexScale = 0;
    int maxIndexScale = copyZValues.length - 1;
    double min = copyZValues[minIndexScale];
    double max = copyZValues[maxIndexScale];
    PaintScaleFactory paintScaleFactoy = new PaintScaleFactory();
    paintScaleFactoy.createColorsForPaintScale(paintScale);
    // contourColors = XYBlockPixelSizePaintScales.scaleAlphaForPaintScale(contourColors);

    plot = chart.getXYPlot();
    plot.getDomainAxis().setLabel(unitFormat.format("Retention time", "min"));
    if (mobilityType != null) {
      plot.getRangeAxis().setLabel(unitFormat.format("Mobility", mobilityType.getUnit()));
    }
    EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    theme.apply(chart);

    setPixelRenderer();
    prepareLegend(min, max, paintScale);

    blockRenderer.setPaintScale(paintScale);
    plot.setRenderer(blockRenderer);
    plot.setBackgroundPaint(Color.black);
    plot.setRangeGridlinePaint(Color.black);
    plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
    plot.setOutlinePaint(Color.black);
    chart.clearSubtitles(); // don't show the dataset label, that should be clear.
    chart.addSubtitle(legend);

  }

  private void setPixelRenderer() {
    blockRenderer = new XYBlockRendererSmallBlocks();
    blockRenderer.setBlockHeight(dataPointHeight);
    blockRenderer.setBlockWidth(dataPointWidth);
  }

  private void prepareLegend(double min, double max, LookupPaintScale scale) {
    NumberAxis scaleAxis = new NumberAxis(null);
    scaleAxis.setNumberFormatOverride(MZmineCore.getConfiguration().getIntensityFormat());
    scaleAxis.setRange(min, max);
    scaleAxis.setAxisLinePaint(Color.white);
    scaleAxis.setTickMarkPaint(Color.white);
    legend = new PaintScaleLegend(scale, scaleAxis);
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
}
