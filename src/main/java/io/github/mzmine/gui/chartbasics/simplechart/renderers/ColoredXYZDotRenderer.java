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
import io.github.mzmine.gui.chartbasics.simplechart.generators.SimpleToolTipGenerator;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

public class ColoredXYZDotRenderer extends XYLineAndShapeRenderer {

  private static final Shape dataPointsShape = new Ellipse2D.Double(-2, -2, 5, 5);

  public ColoredXYZDotRenderer() {
    super(false, true);

    SimpleToolTipGenerator toolTipGenerator = new SimpleToolTipGenerator();
    setDefaultToolTipGenerator(toolTipGenerator);

    setDefaultItemLabelsVisible(false);

    setSeriesVisibleInLegend(0, false);
    setSeriesItemLabelsVisible(0, false);
    setSeriesShape(0, dataPointsShape);

    setDrawSeriesLineAsPath(true);
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
