/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.gui.chartbasics.simplechart.renderers;

import io.github.mzmine.gui.chartbasics.simplechart.providers.ColorProvider;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.SamplingXYLineRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.XYDataset;

/**
 * A line renderer that does not draw every point, but checks first if it would make a difference to
 * draw that point.
 */
public class ColoredXYSamplingLineRenderer extends SamplingXYLineRenderer {

  XYDataset currentDataset;
  private final double dpi = 150;

  @Override
  public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea, XYPlot plot,
      XYDataset data, PlotRenderingInfo info) {

    State state = (State) super.initialise(g2, dataArea, plot, data, info);

    try {
      final Field dX = state.getClass().getDeclaredField("dX");
      dX.setAccessible(true);
      dX.setDouble(state, 72d / dpi);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    return state;
  }

  @Override
  public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea,
      PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis,
      XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {
    currentDataset = dataset;
    super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item,
        crosshairState, pass);
  }

  @Override
  public Paint getSeriesPaint(int series) {
    if (currentDataset instanceof ColorProvider) {
      return ((ColorProvider) currentDataset).getAWTColor();
    }
    return super.getSeriesPaint(series);
  }
}
