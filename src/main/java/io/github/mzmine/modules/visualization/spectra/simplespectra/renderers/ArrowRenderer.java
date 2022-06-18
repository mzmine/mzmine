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

import java.awt.Color;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

/**
 * Renderer for the mass list in spectrum plot
 */
public class ArrowRenderer extends XYLineAndShapeRenderer {


  public static final Ellipse2D circle = new Ellipse2D.Double(-2.5, 5, 5, 5);

  public static final Shape rightArrow = new Polygon(new int[]{0, 0, 3}, new int[]{-3, 3, 0}, 3);
  public static final Shape leftArrow = new Polygon(new int[]{0, 0, -3}, new int[]{-3, 3, 0}, 3);
  public static final Shape downArrow = new Polygon(new int[]{-3, 3, 0}, new int[]{0, 0, 3}, 3);
  public static final Shape upArrow = new Polygon(new int[]{-3, 3, 0}, new int[]{3, 3, 0}, 3);
  public static final Shape diamond = new Polygon(new int[]{0, -3, 0, 3}, new int[]{0, 3, 6, 3}, 4);
  private final ShapeType shapeType;

  private final Color[] color;

  public ArrowRenderer(Color... color) {
    this(ShapeType.CIRCLE, color);
  }


  public ArrowRenderer(ShapeType shapeType, Color... color) {
    this(shapeType, circle, color);
  }

  public ArrowRenderer(ShapeType shapeType, Shape shape, Color... color) {
    super(false, true);
    this.color = color;
    this.shapeType = shapeType;
    setDefaultShape(shape);
    setDefaultSeriesVisibleInLegend(false);
  }

  public ShapeType getShapeType() {
    return shapeType;
  }

  @Override
  public Shape getItemShape(int series, int item) {
    return getDefaultShape();
  }

  public enum ShapeType {
    UP, DOWN, LEFT, RIGHT, DIAMOND, CIRCLE
  }

  @Override
  public Paint getItemPaint(int series, int item) {
    return series < color.length ? color[series] : color[0];
  }

}
