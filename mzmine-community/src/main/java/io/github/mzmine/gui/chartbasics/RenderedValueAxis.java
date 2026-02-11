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

package io.github.mzmine.gui.chartbasics;

import java.awt.geom.Rectangle2D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;

public record RenderedValueAxis(@NotNull ValueAxis axis, @NotNull RectangleEdge edge) {

  public static @Nullable RenderedValueAxis of(@Nullable ValueAxis axis,
      @Nullable RectangleEdge edge) {
    if (axis == null || edge == null) {
      return null;
    }
    return new RenderedValueAxis(axis, edge);
  }

  @Nullable
  public static RenderedValueAxis domainOf(@NotNull XYPlot plot) {
    ValueAxis domainAxis = plot.getDomainAxis();
    RectangleEdge domainAxisEdge = plot.getDomainAxisEdge();
    // parent?
    if (domainAxis == null && plot.getParent() != null && plot.getParent() instanceof XYPlot pp) {
      domainAxis = pp.getDomainAxis();
      domainAxisEdge = pp.getDomainAxisEdge();
    }
    return RenderedValueAxis.of(domainAxis, domainAxisEdge);
  }


  public static RenderedValueAxis rangeOf(XYPlot plot) {
    ValueAxis rangeAxis = plot.getRangeAxis();
    RectangleEdge rangeAxisEdge = plot.getRangeAxisEdge();
    if (rangeAxis == null && plot.getParent() != null && plot.getParent() instanceof XYPlot pp) {
      rangeAxis = pp.getRangeAxis();
      rangeAxisEdge = pp.getRangeAxisEdge();
    }
    return RenderedValueAxis.of(rangeAxis, rangeAxisEdge);
  }

  public double java2DToValue(double coordinate, Rectangle2D dataArea) {
    return axis.java2DToValue(coordinate, dataArea, edge);
  }

  public double valueToJava2D(double coordinate, Rectangle2D dataArea) {
    return axis.valueToJava2D(coordinate, dataArea, edge);
  }
}
