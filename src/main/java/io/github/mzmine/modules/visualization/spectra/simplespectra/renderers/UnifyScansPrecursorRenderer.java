/*
 * Copyright 2006-2021 The MZmine Development Team
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

import java.awt.Color;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Shape;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

/**
 * Renderer for the mass list in spectrum plot
 */
public class UnifyScansPrecursorRenderer extends XYLineAndShapeRenderer {

  private static final Shape rightArrow;
  private static final Shape leftArrow;
  private static final Shape downArrow;

  static {
    rightArrow = new Polygon(new int[]{0, 0, 1}, new int[]{-1, 1, 0}, 3);
    leftArrow = new Polygon(new int[]{0, 0, -1}, new int[]{-1, 1, 0}, 3);
    downArrow = new Polygon(new int[]{-1, 1, 0}, new int[]{1, 1, 0}, 3);
  }

  private final Color color;

  public UnifyScansPrecursorRenderer(Color color) {
    super(false, true);

    this.color = color;
  }

  @Override
  public Shape getItemShape(int series, int item) {
    // sequence of lower and upper limit of precursor selection windows
    return item % 2 == 0 ? rightArrow : leftArrow;
  }

  @Override
  public Paint getItemPaint(int row, int column) {
    return color;
  }

}
