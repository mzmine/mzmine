/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYDataset;

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

  @Nullable
  public static RenderedValueAxis domainOfDataset(final @NotNull XYPlot plot,
      final @NotNull XYDataset dataset) {
    final int datasetIndex = plot.indexOf(dataset);
    ValueAxis domainAxis = datasetIndex >= 0 ? plot.getDomainAxisForDataset(datasetIndex) : null;
    if (domainAxis == null) {
      return domainOf(plot);
    }

    final int axisIndex = findDomainAxisIndex(plot, domainAxis);
    final RectangleEdge edge =
        axisIndex >= 0 ? Plot.resolveDomainAxisLocation(plot.getDomainAxisLocation(axisIndex),
            plot.getOrientation()) : plot.getDomainAxisEdge();
    return RenderedValueAxis.of(domainAxis, edge);
  }

  @Nullable
  public static RenderedValueAxis rangeOfDataset(final @NotNull XYPlot plot,
      final @NotNull XYDataset dataset) {
    final int datasetIndex = plot.indexOf(dataset);
    ValueAxis rangeAxis = datasetIndex >= 0 ? plot.getRangeAxisForDataset(datasetIndex) : null;
    if (rangeAxis == null) {
      return rangeOf(plot);
    }

    final int axisIndex = findRangeAxisIndex(plot, rangeAxis);
    final RectangleEdge edge =
        axisIndex >= 0 ? Plot.resolveRangeAxisLocation(plot.getRangeAxisLocation(axisIndex),
            plot.getOrientation()) : plot.getRangeAxisEdge();
    return RenderedValueAxis.of(rangeAxis, edge);
  }

  private static int findDomainAxisIndex(final @NotNull XYPlot plot,
      final @NotNull ValueAxis axis) {
    for (int i = 0; i < plot.getDomainAxisCount(); i++) {
      if (plot.getDomainAxis(i) == axis) {
        return i;
      }
    }
    return plot.getDomainAxis() == axis ? 0 : -1;
  }

  private static int findRangeAxisIndex(final @NotNull XYPlot plot, final @NotNull ValueAxis axis) {
    for (int i = 0; i < plot.getRangeAxisCount(); i++) {
      if (plot.getRangeAxis(i) == axis) {
        return i;
      }
    }
    return plot.getRangeAxis() == axis ? 0 : -1;
  }

  public double java2DToValue(double coordinate, Rectangle2D dataArea) {
    return axis.java2DToValue(coordinate, dataArea, edge);
  }

  public double valueToJava2D(double coordinate, Rectangle2D dataArea) {
    return axis.valueToJava2D(coordinate, dataArea, edge);
  }
}
