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

package io.github.mzmine.modules.visualization.chromatogram;

import io.github.mzmine.gui.chartbasics.simplechart.SimpleChartUtility;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

public class TICPlotRenderer extends XYLineAndShapeRenderer {

  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private final double transparency = 1.0f;

  public TICPlotRenderer() {
    super(true, false);
    // draw the whole line as one path and not disjoint segments
    setDrawSeriesLineAsPath(true);
    SimpleChartUtility.tryApplyDefaultChartThemeToRenderer(this);
  }

  private AlphaComposite makeComposite(double alpha) {
    int type = AlphaComposite.SRC_OVER;
    return (AlphaComposite.getInstance(type, (float) alpha));
  }

  public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea,
      PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis,
      XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {

    g2.setComposite(makeComposite(transparency));

    super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item,
        crosshairState, pass);

  }

  protected void drawPrimaryLine(XYItemRendererState state, Graphics2D g2, XYPlot plot,
      XYDataset dataset, int pass, int series, int item, ValueAxis domainAxis, ValueAxis rangeAxis,
      Rectangle2D dataArea) {

    g2.setComposite(makeComposite(transparency));

    super.drawPrimaryLine(state, g2, plot, dataset, pass, series, item, domainAxis, rangeAxis,
        dataArea);

  }

  protected void drawFirstPassShape(Graphics2D g2, int pass, int series, int item, Shape shape) {
    g2.setComposite(makeComposite(transparency));
    g2.setStroke(getItemStroke(series, item));
    g2.setPaint(getItemPaint(series, item));
    g2.draw(shape);
  }

  protected void drawPrimaryLineAsPath(XYItemRendererState state, Graphics2D g2, XYPlot plot,
      XYDataset dataset, int pass, int series, int item, ValueAxis domainAxis, ValueAxis rangeAxis,
      Rectangle2D dataArea) {

    g2.setComposite(makeComposite(transparency));

    super.drawPrimaryLineAsPath(state, g2, plot, dataset, pass, series, item, domainAxis, rangeAxis,
        dataArea);

  }

  protected void drawSecondaryPass(Graphics2D g2, XYPlot plot, XYDataset dataset, int pass,
      int series, int item, ValueAxis domainAxis, Rectangle2D dataArea, ValueAxis rangeAxis,
      CrosshairState crosshairState, EntityCollection entities) {

    g2.setComposite(makeComposite(transparency));

    super.drawSecondaryPass(g2, plot, dataset, pass, series, item, domainAxis, dataArea, rangeAxis,
        crosshairState, entities);

  }

}
