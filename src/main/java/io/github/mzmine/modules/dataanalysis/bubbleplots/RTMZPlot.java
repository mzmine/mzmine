/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataanalysis.bubbleplots;

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
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.listener.ZoomHistory;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;

public class RTMZPlot extends EChartViewer {

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
    // setMaximumDrawWidth(Integer.MAX_VALUE);
    // setMaximumDrawHeight(Integer.MAX_VALUE);

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
