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

package io.github.mzmine.modules.visualization.spectra.simplespectra.renderers;

import io.github.mzmine.gui.chartbasics.simplechart.SimpleChartUtility;
import io.github.mzmine.main.MZmineCore;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.Serial;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

public class ContinuousRenderer extends XYLineAndShapeRenderer {

  public static final float TRANSPARENCY = 0.8f;
  public static final AlphaComposite alphaComp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
      TRANSPARENCY);

  /**
   *
   */
  @Serial
  private static final long serialVersionUID = 1L;

  // data points shape
  private static final Shape dataPointsShape = new Ellipse2D.Double(-2, -2, 5, 5);
  private final boolean isTransparent;

  public ContinuousRenderer(Color color, boolean isTransparent) {

    this.isTransparent = isTransparent;

    // Set painting color
    setDefaultPaint(color);
    setDefaultFillPaint(color);
    setUseFillPaint(true);
    setDefaultStroke(MZmineCore.getConfiguration().getDefaultChartTheme().getDefaultDataStroke());
    setDefaultOutlineStroke(
        MZmineCore.getConfiguration().getDefaultChartTheme().getDefaultDataStroke());

    // Set shape properties
    setDefaultShape(dataPointsShape);
    setDefaultShapesFilled(true);
    setDefaultShapesVisible(false);
    setDrawOutlines(false);

    // Set the tooltip generator
    SpectraToolTipGenerator tooltipGenerator = new SpectraToolTipGenerator();
    setDefaultToolTipGenerator(tooltipGenerator);

    setDrawSeriesLineAsPath(true);
    SimpleChartUtility.tryApplyDefaultChartThemeToRenderer(this);
  }

  public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea,
      PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis,
      XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {

    if (isTransparent) {
      g2.setComposite(alphaComp);
    }

    super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item,
        crosshairState, pass);

  }

  /**
   * This method returns null, because we don't want to change the colors dynamically.
   */
  public DrawingSupplier getDrawingSupplier() {
    return null;
  }

}
