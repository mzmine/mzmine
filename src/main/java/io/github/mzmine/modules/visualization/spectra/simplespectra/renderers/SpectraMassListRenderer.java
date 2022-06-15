/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.spectra.simplespectra.renderers;

import io.github.mzmine.gui.chartbasics.simplechart.SimpleChartUtility;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

/**
 * Renderer for the mass list in spectrum plot
 */
public class SpectraMassListRenderer extends XYLineAndShapeRenderer {

  private static final Shape dataPointShape = new Ellipse2D.Double(-2.5, -2.5, 5, 5);
  private final Color color;

  public SpectraMassListRenderer(Color color) {
    super(false, true);

    this.color = color;
    setDefaultPaint(color);
    SimpleChartUtility.tryApplyDefaultChartThemeToRenderer(this);
  }

  @Override
  public Shape getItemShape(int row, int col) {
    return dataPointShape;
  }

  @Override
  public Paint getItemPaint(int row, int column) {
    return color;
  }

}
