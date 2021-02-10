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

package io.github.mzmine.gui.chartbasics.simplechart.renderers;

import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYDataset;

public class ColoredXYZDotRenderer extends XYDotRenderer {

  private static final int DOT_WIDTH = 5;
  private static final int DOT_HEIGHT = 5;

  public ColoredXYZDotRenderer() {
    super();
  }

  @Override
  public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea,
      PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis,
      XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {
    if (this.getItemVisible(series, item)) {
      double x = dataset.getXValue(series, item);
      double y = dataset.getYValue(series, item);
      double adjx = (double)(this.DOT_WIDTH - 1) / 2.0D;
      double adjy = (double)(this.DOT_HEIGHT - 1) / 2.0D;
      if (!java.lang.Double.isNaN(y)) {
        RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
        RectangleEdge yAxisLocation = plot.getRangeAxisEdge();
        double transX = domainAxis.valueToJava2D(x, dataArea, xAxisLocation) - adjx;
        double transY = rangeAxis.valueToJava2D(y, dataArea, yAxisLocation) - adjy;
        g2.setPaint(this.getItemPaint(series, item));
        PlotOrientation orientation = plot.getOrientation();
        if (orientation == PlotOrientation.HORIZONTAL) {
          g2.fillOval((int)transY, (int)transX, this.DOT_HEIGHT, this.DOT_WIDTH);
        } else if (orientation == PlotOrientation.VERTICAL) {
          g2.fillOval((int)transX, (int)transY, this.DOT_WIDTH, this.DOT_HEIGHT);
        }

        int datasetIndex = plot.indexOf(dataset);
        this.updateCrosshairValues(crosshairState, x, y, datasetIndex, transX, transY, orientation);
      }
    }
  }

  @Override
  public Paint getItemPaint(int row, int col) {
    double zValue = ((ColoredXYZDataset) getPlot().getDataset()).getZValue(row, col);

    if (zValue > 1) {
      int rgbVal = (int) Math.round(220 - (zValue - 1) * 220);
      return new Color(255, rgbVal, rgbVal);
    } else {
      int rgbVal = (int) Math.round(220 - zValue * 220);
      return new Color(rgbVal, rgbVal, 255);
    }
  }

}
