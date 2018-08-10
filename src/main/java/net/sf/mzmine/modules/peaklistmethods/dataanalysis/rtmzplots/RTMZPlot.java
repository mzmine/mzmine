/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.rtmzplots;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.text.NumberFormat;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.AbstractXYZDataset;
import net.sf.mzmine.chartbasics.EChartPanel;
import net.sf.mzmine.chartbasics.listener.ZoomHistory;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;

public class RTMZPlot extends EChartPanel {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private static final Color gridColor = Color.lightGray;
  private static final Color crossHairColor = Color.gray;
  private static final Font titleFont = new Font("SansSerif", Font.PLAIN, 11);
  // crosshair stroke
  private static final BasicStroke crossHairStroke =
      new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[] {5, 3}, 0);

  private JFreeChart chart;
  private XYPlot plot;
  private ValueAxis paintScaleAxis;
  private PaintScaleLegend paintScaleLegend;

  private XYItemRenderer spotRenderer;

  private InterpolatingLookupPaintScale paintScale;

  public RTMZPlot(RTMZAnalyzerWindow masterFrame, AbstractXYZDataset dataset,
      InterpolatingLookupPaintScale paintScale) {
    super(null);

    this.paintScale = paintScale;

    chart = ChartFactory.createXYAreaChart("", "Retention time", "m/z", dataset,
        PlotOrientation.VERTICAL, false, false, false);
    chart.setBackgroundPaint(Color.white);
    setChart(chart);

    // title

    TextTitle chartTitle = chart.getTitle();
    chartTitle.setMargin(5, 0, 0, 0);
    chartTitle.setFont(titleFont);
    chart.removeSubtitle(chartTitle);

    // disable maximum size (we don't want scaling)
    setMaximumDrawWidth(Integer.MAX_VALUE);
    setMaximumDrawHeight(Integer.MAX_VALUE);

    // set the plot properties
    plot = chart.getXYPlot();
    plot.setBackgroundPaint(Color.white);
    plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

    // set grid properties
    plot.setDomainGridlinePaint(gridColor);
    plot.setRangeGridlinePaint(gridColor);

    // set crosshair (selection) properties
    plot.setDomainCrosshairVisible(true);
    plot.setRangeCrosshairVisible(true);
    plot.setDomainCrosshairPaint(crossHairColor);
    plot.setRangeCrosshairPaint(crossHairColor);
    plot.setDomainCrosshairStroke(crossHairStroke);
    plot.setRangeCrosshairStroke(crossHairStroke);

    NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

    // set the X axis (retention time) properties
    NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
    xAxis.setNumberFormatOverride(rtFormat);
    xAxis.setUpperMargin(0.001);
    xAxis.setLowerMargin(0.001);

    // set the Y axis (intensity) properties
    NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
    yAxis.setAutoRangeIncludesZero(false);
    yAxis.setNumberFormatOverride(mzFormat);

    plot.setDataset(dataset);
    spotRenderer = new RTMZRenderer(dataset, paintScale);
    plot.setRenderer(spotRenderer);
    spotRenderer.setDefaultToolTipGenerator(new RTMZToolTipGenerator());

    // Add a paintScaleLegend to chart

    paintScaleAxis = new NumberAxis("Logratio");
    paintScaleAxis.setRange(paintScale.getLowerBound(), paintScale.getUpperBound());

    paintScaleLegend = new PaintScaleLegend(paintScale, paintScaleAxis);
    paintScaleLegend.setPosition(plot.getDomainAxisEdge());
    paintScaleLegend.setMargin(5, 25, 5, 25);

    chart.addSubtitle(paintScaleLegend);


    // reset zoom history
    ZoomHistory history = getZoomHistory();
    if (history != null)
      history.clear();
  }

  public InterpolatingLookupPaintScale getPaintScale() {
    return paintScale;
  }

  public void setPaintScale(InterpolatingLookupPaintScale paintScale) {

    RTMZRenderer renderer = (RTMZRenderer) plot.getRenderer();
    renderer.setPaintScale(paintScale);

    this.paintScale = paintScale;
    paintScaleAxis.setRange(paintScale.getLowerBound(), paintScale.getUpperBound());
    paintScaleLegend.setScale(paintScale);
  }

}
